package com.petcare.app.data

import com.petcare.app.model.AVATAR_COLORS
import com.petcare.app.model.AppointmentKind
import com.petcare.app.model.AppointmentStatus
import com.petcare.app.model.Pet
import com.petcare.app.model.VetAppointment
import com.petcare.app.util.DateUtils
import java.time.LocalDate

/** ใส่สัตว์เลี้ยงและนัดหมายตัวอย่างให้บัญชีเดโมที่เพิ่งสร้าง (วันที่อิงจากวันนี้) */
class DemoDataSeeder(
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
) {
    suspend fun seed(today: LocalDate = LocalDate.now()) {
        fun day(offset: Long) = DateUtils.formatDate(today.plusDays(offset))

        val moji = Pet(
            name = "โมจิ", species = "สุนัข", breed = "ชิบะ อินุ",
            birthday = DateUtils.formatDate(today.minusYears(2).minusMonths(4)),
            avatarColor = AVATAR_COLORS[1],
        )
        val mojiId = petRepository.savePet(moji)

        val tofu = Pet(
            name = "เต้าหู้", species = "แมว", breed = "สก็อตติช โฟลด์",
            birthday = DateUtils.formatDate(today.minusYears(1).minusMonths(1)),
            avatarColor = AVATAR_COLORS[0],
        )
        val tofuId = petRepository.savePet(tofu)

        val samples = listOf(
            VetAppointment(
                petId = mojiId, kind = AppointmentKind.VACCINE.name, title = "วัคซีนพิษสุนัขบ้า (กระตุ้นประจำปี)",
                date = day(3), time = "10:30", vetName = "สพ.ญ. ศิริพร", clinic = "โรงพยาบาลสัตว์ใจดี",
                reminderDaysBefore = 3,
            ) to moji.name,
            VetAppointment(
                petId = mojiId, kind = AppointmentKind.MEDICATION.name, title = "หยดยากันเห็บหมัด",
                date = day(-2), time = "09:00", reminderDaysBefore = 1,
            ) to moji.name,
            VetAppointment(
                petId = mojiId, kind = AppointmentKind.VACCINE.name, title = "วัคซีนรวม 5 โรค",
                date = day(-60), time = "14:00", vetName = "สพ.ญ. ศิริพร", clinic = "โรงพยาบาลสัตว์ใจดี",
                status = AppointmentStatus.DONE.name,
            ) to moji.name,
            VetAppointment(
                petId = tofuId, kind = AppointmentKind.CHECKUP.name, title = "ตรวจสุขภาพประจำปี",
                date = day(12), time = "13:00", vetName = "น.สพ. ธนกฤต", clinic = "คลินิกรักษ์แมว",
                notes = "งดอาหารก่อนเจาะเลือด 8 ชั่วโมง", reminderDaysBefore = 2,
            ) to tofu.name,
            VetAppointment(
                petId = tofuId, kind = AppointmentKind.VACCINE.name, title = "วัคซีนไข้หัดแมว",
                date = day(-30), time = "11:00", clinic = "คลินิกรักษ์แมว",
                status = AppointmentStatus.DONE.name,
            ) to tofu.name,
        )
        samples.forEach { (appt, petName) -> appointmentRepository.save(appt, petName) }
    }
}
