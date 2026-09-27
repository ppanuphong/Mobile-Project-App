package com.petcare.app

import com.petcare.app.model.AppointmentFilter
import com.petcare.app.model.AppointmentStatus
import com.petcare.app.model.DisplayStatus
import com.petcare.app.model.VetAppointment
import com.petcare.app.notification.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AppointmentLogicTest {
    private val today = LocalDate.of(2026, 9, 27)

    private fun appt(date: String, reminder: Int = 1, done: Boolean = false) = VetAppointment(
        id = "a1",
        date = date,
        reminderDaysBefore = reminder,
        status = if (done) AppointmentStatus.DONE.name else AppointmentStatus.PENDING.name,
    )

    @Test
    fun displayStatus() {
        assertEquals(DisplayStatus.OVERDUE, appt("2026-09-26").displayStatus(today))
        assertEquals(DisplayStatus.DUE_SOON, appt("2026-09-27").displayStatus(today))
        assertEquals(DisplayStatus.DUE_SOON, appt("2026-10-04").displayStatus(today))
        assertEquals(DisplayStatus.UPCOMING, appt("2026-10-05").displayStatus(today))
        // เตือนล่วงหน้า 14 วัน → นับว่าใกล้ถึงตั้งแต่ 14 วันก่อน
        assertEquals(DisplayStatus.DUE_SOON, appt("2026-10-10", reminder = 14).displayStatus(today))
        assertEquals(DisplayStatus.DONE, appt("2026-09-01", done = true).displayStatus(today))
    }

    @Test
    fun reminderWindow() {
        assertTrue(appt("2026-09-28", reminder = 1).isInReminderWindow(today))
        assertFalse(appt("2026-09-29", reminder = 1).isInReminderWindow(today))
        assertTrue(appt("2026-09-20").isInReminderWindow(today)) // เลยกำหนดยังนับว่าต้องเตือน
        assertFalse(appt("2026-09-28", done = true).isInReminderWindow(today))
    }

    @Test
    fun filterMatches() {
        assertTrue(AppointmentFilter.UPCOMING.matches(DisplayStatus.DUE_SOON))
        assertTrue(AppointmentFilter.UPCOMING.matches(DisplayStatus.UPCOMING))
        assertFalse(AppointmentFilter.UPCOMING.matches(DisplayStatus.OVERDUE))
        assertTrue(AppointmentFilter.ALL.matches(DisplayStatus.DONE))
    }

    @Test
    fun triggerTime_isEightAmOnReminderDay() {
        assertEquals(
            LocalDateTime.of(2026, 10, 7, 8, 0),
            ReminderScheduler.triggerTime(appt("2026-10-10", reminder = 3)),
        )
        assertEquals(
            LocalDateTime.of(2026, 10, 10, 8, 0),
            ReminderScheduler.triggerTime(appt("2026-10-10", reminder = 0)),
        )
    }

    @Test
    fun delay_neverNegative() {
        val now = LocalDateTime.of(2026, 9, 27, 12, 0)
        assertEquals(0L, ReminderScheduler.delayMillis(now.minusHours(1), now))
        assertEquals(3_600_000L, ReminderScheduler.delayMillis(now.plusHours(1), now))
    }
}
