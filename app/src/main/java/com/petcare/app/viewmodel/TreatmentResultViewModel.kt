package com.petcare.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AppointmentKind
import com.petcare.app.model.Pet
import com.petcare.app.model.TreatmentOutcome
import com.petcare.app.model.TreatmentResult
import com.petcare.app.model.VetAppointment
import com.petcare.app.ui.navigation.Routes
import com.petcare.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TreatmentResultUiState(
    val isLoading: Boolean = true,
    val appointment: VetAppointment? = null,
    val pet: Pet? = null,
    val isEdit: Boolean = false,
    val diagnosis: String = "",
    val treatment: String = "",
    val medications: String = "",
    val weightKg: String = "",
    val cost: String = "",
    val outcome: TreatmentOutcome = TreatmentOutcome.NORMAL,
    /** มีนัดติดตามผลอยู่แล้ว (สร้างตอนบันทึกครั้งก่อน) */
    val hasExistingFollowUp: Boolean = false,
    val createFollowUp: Boolean = false,
    val followUpDate: String = DateUtils.formatDate(LocalDate.now().plusDays(7)),
    val followUpTime: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

/**
 * ฟอร์มบันทึกผลการรักษาหลังพบหมอ: ผลวินิจฉัย การรักษา ยา น้ำหนัก ค่าใช้จ่าย ผลลัพธ์
 * และสร้างนัดติดตามผลได้ในขั้นตอนเดียว
 */
class TreatmentResultViewModel(
    savedStateHandle: SavedStateHandle,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val appointmentId: String = checkNotNull(savedStateHandle[Routes.ARG_APPOINTMENT_ID])

    private val _uiState = MutableStateFlow(TreatmentResultUiState())
    val uiState: StateFlow<TreatmentResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val appt = appointmentRepository.observeAppointment(appointmentId).firstOrNull()
            val pet = appt?.let { petRepository.observePet(it.petId).firstOrNull() }
            val result = appt?.result
            val outcome = result?.outcomeEnum ?: defaultOutcome(appt)
            val hasFollowUp = result?.followUpAppointmentId?.isNotBlank() == true
            _uiState.update {
                it.copy(
                    isLoading = false,
                    appointment = appt,
                    pet = pet,
                    isEdit = result != null,
                    diagnosis = result?.diagnosis.orEmpty(),
                    treatment = result?.treatment.orEmpty(),
                    medications = result?.medications.orEmpty(),
                    weightKg = result?.weightKg?.takeIf { w -> w > 0 }?.let(::formatNumber).orEmpty(),
                    cost = result?.cost?.takeIf { c -> c > 0 }?.let(::formatNumber).orEmpty(),
                    outcome = outcome,
                    hasExistingFollowUp = hasFollowUp,
                    // เคสที่ยังต้องรักษา ตั้งนัดติดตามผลไว้ให้ก่อน (ปิดเองได้)
                    createFollowUp = outcome == TreatmentOutcome.ONGOING && !hasFollowUp,
                    followUpTime = appt?.time.orEmpty(),
                    errorMessage = if (appt == null) "ไม่พบนัดหมายนี้" else null,
                )
            }
        }
    }

    // วัคซีน/อาบน้ำ มักไม่ต้องติดตามผล ส่วนการรักษาโรคตั้งต้นเป็น "อยู่ระหว่างรักษา"
    private fun defaultOutcome(appt: VetAppointment?) = when (appt?.kindEnum) {
        AppointmentKind.TREATMENT, AppointmentKind.MEDICATION -> TreatmentOutcome.ONGOING
        else -> TreatmentOutcome.NORMAL
    }

    private fun edit(field: String, change: TreatmentResultUiState.() -> TreatmentResultUiState) =
        _uiState.update { it.change().copy(errors = it.errors - field) }

    fun onDiagnosisChange(v: String) = edit(FIELD_DIAGNOSIS) { copy(diagnosis = v) }
    fun onTreatmentChange(v: String) = edit(FIELD_TREATMENT) { copy(treatment = v) }
    fun onMedicationsChange(v: String) = edit(FIELD_MEDICATIONS) { copy(medications = v) }
    fun onWeightChange(v: String) = edit(FIELD_WEIGHT) { copy(weightKg = v.filter { it.isDigit() || it == '.' }) }
    fun onCostChange(v: String) = edit(FIELD_COST) { copy(cost = v.filter { it.isDigit() || it == '.' }) }

    /** เลือก "อยู่ระหว่างรักษา" แล้วเปิดนัดติดตามผลให้อัตโนมัติ (ปิดเองได้) */
    fun onOutcomeChange(v: TreatmentOutcome) = _uiState.update {
        it.copy(outcome = v, createFollowUp = if (v == TreatmentOutcome.ONGOING && !it.hasExistingFollowUp) true else it.createFollowUp)
    }

    fun onCreateFollowUpChange(v: Boolean) = _uiState.update { it.copy(createFollowUp = v) }
    fun onFollowUpDateChange(v: String) = edit(FIELD_FOLLOW_UP_DATE) { copy(followUpDate = v) }
    fun onFollowUpTimeChange(v: String) = _uiState.update { it.copy(followUpTime = v) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun save() {
        val s = _uiState.value
        val appt = s.appointment ?: return
        if (s.isSaving) return

        val weight = s.weightKg.toDoubleOrNull()
        val cost = s.cost.toDoubleOrNull()
        val wantsFollowUp = s.createFollowUp && !s.hasExistingFollowUp
        val followUpDay = DateUtils.parseDate(s.followUpDate)
        val errors = buildMap {
            if (s.diagnosis.isBlank() && s.treatment.isBlank()) {
                put(FIELD_DIAGNOSIS, "กรอกผลวินิจฉัยหรือการรักษาอย่างน้อย 1 ช่อง")
            }
            if (s.weightKg.isNotBlank() && (weight == null || weight <= 0 || weight > 500)) put(FIELD_WEIGHT, "น้ำหนักไม่ถูกต้อง")
            if (s.cost.isNotBlank() && cost == null) put(FIELD_COST, "จำนวนเงินไม่ถูกต้อง")
            if (wantsFollowUp && (followUpDay == null || followUpDay.isBefore(LocalDate.now()))) {
                put(FIELD_FOLLOW_UP_DATE, "เลือกวันนัดติดตามผลตั้งแต่วันนี้เป็นต้นไป")
            }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        val result = (appt.result ?: TreatmentResult()).copy(
            diagnosis = s.diagnosis.trim(),
            treatment = s.treatment.trim(),
            medications = s.medications.trim(),
            weightKg = weight ?: 0.0,
            cost = cost ?: 0.0,
            outcome = s.outcome.name,
            recordedAt = appt.result?.recordedAt?.takeIf { it.isNotBlank() } ?: DateUtils.formatDate(LocalDate.now()),
        )
        val followUp = if (wantsFollowUp) followUpFor(appt, s) else null

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { appointmentRepository.recordResult(appt, result, followUp, s.pet?.name.orEmpty()) }
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = "บันทึกไม่สำเร็จ: ${e.message}") } }
        }
    }

    private fun followUpFor(appt: VetAppointment, s: TreatmentResultUiState) = VetAppointment(
        petId = appt.petId,
        kind = AppointmentKind.CHECKUP.name,
        title = "ติดตามผล: ${appt.title}",
        date = s.followUpDate,
        time = s.followUpTime,
        vetName = appt.vetName,
        clinic = appt.clinic,
        notes = s.diagnosis.takeIf { it.isNotBlank() }?.let { "จากผลวินิจฉัย: $it" }.orEmpty(),
        reminderDaysBefore = 1,
    )

    companion object {
        const val FIELD_DIAGNOSIS = "diagnosis"
        const val FIELD_TREATMENT = "treatment"
        const val FIELD_MEDICATIONS = "medications"
        const val FIELD_WEIGHT = "weight"
        const val FIELD_COST = "cost"
        const val FIELD_FOLLOW_UP_DATE = "follow_up_date"

        /** 12.0 → "12", 4.5 → "4.5" */
        fun formatNumber(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
