package com.petcare.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.petcare.app.PetCareApp
import com.petcare.app.util.DateUtils

/** ทำงานเมื่อถึงเวลาเตือน แล้วแสดง Notification ของระบบ Android */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as PetCareApp).container
        if (!container.sessionManager.isNotificationsEnabled()) return Result.success()
        if (container.sessionManager.currentSession() == null) return Result.success()

        val id = inputData.getString(KEY_APPOINTMENT_ID) ?: return Result.success()
        val petName = inputData.getString(KEY_PET_NAME).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val kind = inputData.getString(KEY_KIND).orEmpty()
        val date = inputData.getString(KEY_DATE).orEmpty()
        val time = inputData.getString(KEY_TIME).orEmpty()
        val clinic = inputData.getString(KEY_CLINIC).orEmpty()

        val whenText = buildString {
            append(DateUtils.relativeLabel(date))
            append(" · ")
            append(DateUtils.toThaiDateWithDay(date))
            if (time.isNotBlank()) append(" เวลา $time น.")
        }
        val header = if (petName.isNotBlank()) "$kind ของ$petName" else kind
        val body = buildString {
            append(title.ifBlank { kind })
            append("\n")
            append(whenText)
            if (clinic.isNotBlank()) append("\nที่ $clinic")
        }

        NotificationHelper.show(applicationContext, id.hashCode(), header, body)
        return Result.success()
    }

    companion object {
        const val KEY_APPOINTMENT_ID = "appointment_id"
        const val KEY_PET_NAME = "pet_name"
        const val KEY_TITLE = "title"
        const val KEY_KIND = "kind"
        const val KEY_DATE = "date"
        const val KEY_TIME = "time"
        const val KEY_CLINIC = "clinic"
    }
}
