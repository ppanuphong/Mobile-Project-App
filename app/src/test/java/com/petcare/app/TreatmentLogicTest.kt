package com.petcare.app

import com.petcare.app.model.AppointmentStatus
import com.petcare.app.model.PetCondition
import com.petcare.app.model.ProgressUpdate
import com.petcare.app.model.TreatmentOutcome
import com.petcare.app.model.TreatmentResult
import com.petcare.app.model.VetAppointment
import com.petcare.app.model.latestWeightKg
import com.petcare.app.viewmodel.TreatmentResultViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatmentLogicTest {

    private val ongoing = VetAppointment(
        id = "a1",
        date = "2026-09-29",
        status = AppointmentStatus.DONE.name,
        result = TreatmentResult(diagnosis = "ผิวหนังอักเสบ", outcome = TreatmentOutcome.ONGOING.name),
    )

    private fun update(id: String, date: String, condition: PetCondition) =
        ProgressUpdate(id = id, date = date, condition = condition.name)

    @Test
    fun progress_betterKeepsCaseOpen_recoveredClosesIt() {
        val better = ongoing.withProgress(update("p1", "2026-10-01", PetCondition.BETTER))
        assertTrue(better.isUnderTreatment)
        assertEquals(1, better.progress.size)

        val recovered = better.withProgress(update("p2", "2026-10-05", PetCondition.RECOVERED))
        assertFalse(recovered.isUnderTreatment)
        assertEquals(TreatmentOutcome.RECOVERED, recovered.result?.outcomeEnum)
        assertEquals("ผิวหนังอักเสบ", recovered.result?.diagnosis) // ข้อมูลผลเดิมยังอยู่
    }

    @Test
    fun progress_worseReopensClosedCase() {
        val closed = ongoing.copy(result = ongoing.result?.copy(outcome = TreatmentOutcome.RECOVERED.name))
        val relapse = closed.withProgress(update("p1", "2026-10-08", PetCondition.WORSE))
        assertTrue(relapse.isUnderTreatment)
    }

    @Test
    fun latestProgress_isMostRecentDateNotInsertionOrder() {
        val appt = ongoing
            .withProgress(update("p1", "2026-10-05", PetCondition.BETTER))
            .withProgress(update("p2", "2026-10-02", PetCondition.STABLE)) // บันทึกย้อนหลัง
        assertEquals("p1", appt.latestProgress?.id)
        assertEquals(listOf("p1"), appt.withoutProgress("p2").progress.map { it.id })
    }

    @Test
    fun latestWeight_usesNewestRecordedWeight() {
        val records = listOf(
            VetAppointment(date = "2026-07-01", result = TreatmentResult(weightKg = 9.2)),
            VetAppointment(date = "2026-09-01", result = TreatmentResult(weightKg = 9.8)),
            VetAppointment(date = "2026-10-01", result = TreatmentResult(weightKg = 0.0)), // ไม่ได้ชั่ง
            VetAppointment(date = "2026-10-05"),
        )
        assertEquals(9.8, records.latestWeightKg()!!, 0.0)
        assertNull(emptyList<VetAppointment>().latestWeightKg())
    }

    @Test
    fun formatNumber() {
        assertEquals("12", TreatmentResultViewModel.formatNumber(12.0))
        assertEquals("4.5", TreatmentResultViewModel.formatNumber(4.5))
    }
}
