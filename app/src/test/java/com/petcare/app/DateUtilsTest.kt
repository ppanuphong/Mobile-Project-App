package com.petcare.app

import com.petcare.app.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {
    private val today = LocalDate.of(2026, 9, 27)

    @Test
    fun thaiDate_usesBuddhistYearAndThaiMonth() {
        assertEquals("27 ก.ย. 2569", DateUtils.toThaiDate("2026-09-27"))
        assertEquals("อา. 27 ก.ย. 2569", DateUtils.toThaiDateWithDay("2026-09-27"))
    }

    @Test
    fun daysUntil_handlesPastFutureAndInvalid() {
        assertEquals(3L, DateUtils.daysUntil("2026-09-30", today))
        assertEquals(-2L, DateUtils.daysUntil("2026-09-25", today))
        assertNull(DateUtils.daysUntil("not-a-date", today))
    }

    @Test
    fun relativeLabel() {
        assertEquals("วันนี้", DateUtils.relativeLabel("2026-09-27", today))
        assertEquals("พรุ่งนี้", DateUtils.relativeLabel("2026-09-28", today))
        assertEquals("อีก 5 วัน", DateUtils.relativeLabel("2026-10-02", today))
        assertEquals("เลยมา 3 วัน", DateUtils.relativeLabel("2026-09-24", today))
    }

    @Test
    fun ageLabel() {
        assertEquals("2 ปี 4 เดือน", DateUtils.ageLabel("2024-05-27", today))
        assertEquals("3 เดือน", DateUtils.ageLabel("2026-06-20", today))
        assertEquals("ไม่ทราบอายุ", DateUtils.ageLabel("", today))
    }
}
