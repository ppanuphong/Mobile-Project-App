package com.petcare.app.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

object DateUtils {
    private val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val THAI_MONTHS_SHORT = listOf(
        "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
        "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.",
    )
    private val THAI_DAYS_SHORT = listOf("จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.", "อา.")

    fun parseDate(value: String): LocalDate? =
        try { LocalDate.parse(value, DATE) } catch (_: DateTimeParseException) { null }

    fun parseTime(value: String): LocalTime? =
        try { LocalTime.parse(value, TIME) } catch (_: DateTimeParseException) { null }

    fun formatDate(date: LocalDate): String = date.format(DATE)

    fun formatTime(time: LocalTime): String = time.format(TIME)

    /** "2026-09-27" → "27 ก.ย. 2569" */
    fun toThaiDate(value: String): String {
        val d = parseDate(value) ?: return value
        return "${d.dayOfMonth} ${THAI_MONTHS_SHORT[d.monthValue - 1]} ${d.year + 543}"
    }

    /** "2026-09-27" → "อา. 27 ก.ย. 2569" */
    fun toThaiDateWithDay(value: String): String {
        val d = parseDate(value) ?: return value
        return "${THAI_DAYS_SHORT[d.dayOfWeek.value - 1]} ${toThaiDate(value)}"
    }

    fun thaiMonthShort(date: LocalDate): String = THAI_MONTHS_SHORT[date.monthValue - 1]

    /** จำนวนวันจาก [today] ถึง [value] (ติดลบถ้าผ่านมาแล้ว), null ถ้าวันที่ไม่ถูกต้อง */
    fun daysUntil(value: String, today: LocalDate = LocalDate.now()): Long? =
        parseDate(value)?.let { ChronoUnit.DAYS.between(today, it) }

    /** ข้อความสั้น ๆ บอกระยะเวลา เช่น "วันนี้", "อีก 3 วัน", "เลยมา 2 วัน" */
    fun relativeLabel(value: String, today: LocalDate = LocalDate.now()): String {
        val days = daysUntil(value, today) ?: return ""
        return when {
            days == 0L -> "วันนี้"
            days == 1L -> "พรุ่งนี้"
            days > 1 -> "อีก $days วัน"
            days == -1L -> "เมื่อวาน"
            else -> "เลยมา ${-days} วัน"
        }
    }

    /** ข้อความบอกว่าผ่านมานานแค่ไหน เช่น "วันนี้", "เมื่อวาน", "3 วันก่อน" */
    fun agoLabel(value: String, today: LocalDate = LocalDate.now()): String {
        val days = -(daysUntil(value, today) ?: return "")
        return when {
            days <= 0L -> "วันนี้"
            days == 1L -> "เมื่อวาน"
            else -> "$days วันก่อน"
        }
    }

    /** อายุจากวันเกิด เช่น "2 ปี 3 เดือน" */
    fun ageLabel(birthday: String, today: LocalDate = LocalDate.now()): String {
        val b = parseDate(birthday) ?: return "ไม่ทราบอายุ"
        if (b.isAfter(today)) return "ยังไม่เกิด"
        val p = Period.between(b, today)
        return when {
            p.years > 0 && p.months > 0 -> "${p.years} ปี ${p.months} เดือน"
            p.years > 0 -> "${p.years} ปี"
            p.months > 0 -> "${p.months} เดือน"
            else -> "${p.days} วัน"
        }
    }
}
