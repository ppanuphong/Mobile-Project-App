package com.petcare.app.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.petcare.app.model.VetAppointment
import com.petcare.app.util.DateUtils
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * ตั้งแจ้งเตือนล่วงหน้าด้วย WorkManager
 * นัดแต่ละรายการมีงาน 1 ชิ้นชื่อ "appt_<id>" จะทำงานเวลา 08:00 ของวัน (วันนัด - reminderDaysBefore)
 */
class ReminderScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * ตั้ง/อัปเดตการเตือนของนัด 1 รายการ (เรียกหลังบันทึก)
     * ถ้าเลยเวลาเตือนไปแล้วแต่ยังไม่ถึงวันนัด จะเตือนทันที
     */
    fun schedule(appointment: VetAppointment, petName: String, now: LocalDateTime = LocalDateTime.now()) {
        if (appointment.isDone || appointment.id.isBlank()) {
            cancel(appointment.id)
            return
        }
        val trigger = triggerTime(appointment) ?: return
        val appointmentDay = DateUtils.parseDate(appointment.date) ?: return
        if (appointmentDay.isBefore(now.toLocalDate())) {
            cancel(appointment.id)
            return
        }
        if (trigger.isAfter(now)) {
            enqueue(appointment, petName, delayMillis(trigger, now))
        } else {
            // ถึงช่วงเตือนแล้ว: เตือนทันที ใช้ชื่องานแยกและไม่ติด TAG เพื่อไม่ให้ syncAll ยกเลิก
            enqueue(appointment, petName, 0, immediate = true)
        }
    }

    /**
     * ซิงก์การเตือนทั้งหมดกับข้อมูลล่าสุดจาก Firestore (รวมที่แก้จากเครื่องอื่น)
     * ตั้งใหม่เฉพาะนัดที่เวลาเตือนยังไม่มาถึง เพื่อไม่ให้เตือนซ้ำทุกครั้งที่เปิดแอป
     */
    fun syncAll(
        appointments: List<VetAppointment>,
        petNames: Map<String, String>,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        workManager.cancelAllWorkByTag(TAG)
        appointments.forEach { appt ->
            if (appt.isDone || appt.id.isBlank()) return@forEach
            val trigger = triggerTime(appt) ?: return@forEach
            if (trigger.isAfter(now)) {
                enqueue(appt, petNames[appt.petId].orEmpty(), delayMillis(trigger, now))
            }
        }
    }

    fun cancel(appointmentId: String) {
        if (appointmentId.isBlank()) return
        workManager.cancelUniqueWork(workName(appointmentId))
        workManager.cancelUniqueWork(immediateWorkName(appointmentId))
    }

    /** ใช้ตอนออกจากระบบ */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG)
    }

    private fun enqueue(
        appt: VetAppointment,
        petName: String,
        delayMillis: Long,
        immediate: Boolean = false,
    ) {
        val builder = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        if (!immediate) builder.addTag(TAG)
        val request = builder
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_APPOINTMENT_ID to appt.id,
                    ReminderWorker.KEY_PET_NAME to petName,
                    ReminderWorker.KEY_TITLE to appt.title,
                    ReminderWorker.KEY_KIND to appt.kindEnum.label,
                    ReminderWorker.KEY_DATE to appt.date,
                    ReminderWorker.KEY_TIME to appt.time,
                    ReminderWorker.KEY_CLINIC to appt.clinic,
                )
            )
            .build()
        val name = if (immediate) immediateWorkName(appt.id) else workName(appt.id)
        workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val TAG = "appointment_reminder"
        val REMINDER_TIME: LocalTime = LocalTime.of(8, 0)

        fun workName(appointmentId: String) = "appt_$appointmentId"
        fun immediateWorkName(appointmentId: String) = "appt_now_$appointmentId"

        /** เวลาที่ควรเตือน = 08:00 ของ (วันนัด - reminderDaysBefore) */
        fun triggerTime(appt: VetAppointment): LocalDateTime? {
            val date = DateUtils.parseDate(appt.date) ?: return null
            return date.minusDays(appt.reminderDaysBefore.coerceAtLeast(0).toLong()).atTime(REMINDER_TIME)
        }

        fun delayMillis(trigger: LocalDateTime, now: LocalDateTime): Long =
            Duration.between(now, trigger).toMillis().coerceAtLeast(0)
    }
}
