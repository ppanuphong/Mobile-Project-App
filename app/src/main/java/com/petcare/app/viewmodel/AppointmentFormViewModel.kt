package com.petcare.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AppointmentKind
import com.petcare.app.model.AppointmentStatus
import com.petcare.app.model.Pet
import com.petcare.app.model.VetAppointment
import com.petcare.app.ui.navigation.Routes
import com.petcare.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppointmentFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = true,
    val pets: List<Pet> = emptyList(),
    val petId: String = "",
    val kind: AppointmentKind = AppointmentKind.VACCINE,
    val title: String = "",
    val date: String = DateUtils.formatDate(LocalDate.now().plusDays(7)),
    val time: String = "10:00",
    val vetName: String = "",
    val clinic: String = "",
    val notes: String = "",
    val reminderDaysBefore: Int = 1,
    val isDone: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

/** ฟอร์มเพิ่ม / แก้ไขนัดหมาย (route มี appointmentId = แก้ไข, มี petId = เลือกสัตว์ไว้ให้ก่อน) */
class AppointmentFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val appointmentId: String? =
        savedStateHandle.get<String>(Routes.ARG_APPOINTMENT_ID)?.takeIf { it.isNotBlank() }
    private val presetPetId: String? = savedStateHandle.get<String>(Routes.ARG_PET_ID)?.takeIf { it.isNotBlank() }
    private var original: VetAppointment? = null

    private val _uiState = MutableStateFlow(AppointmentFormUiState(isEdit = appointmentId != null))
    val uiState: StateFlow<AppointmentFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val pets = petRepository.observePets().first()
            val existing = appointmentId?.let { appointmentRepository.observeAppointment(it).firstOrNull() }
            original = existing
            _uiState.update { s ->
                if (existing != null) s.copy(
                    isLoading = false,
                    pets = pets,
                    petId = existing.petId,
                    kind = existing.kindEnum,
                    title = existing.title,
                    date = existing.date,
                    time = existing.time,
                    vetName = existing.vetName,
                    clinic = existing.clinic,
                    notes = existing.notes,
                    reminderDaysBefore = existing.reminderDaysBefore,
                    isDone = existing.isDone,
                ) else s.copy(
                    isLoading = false,
                    pets = pets,
                    petId = presetPetId ?: pets.firstOrNull()?.id.orEmpty(),
                    errorMessage = if (appointmentId != null) "ไม่พบนัดหมายนี้" else null,
                )
            }
        }
    }

    private fun edit(field: String, change: AppointmentFormUiState.() -> AppointmentFormUiState) =
        _uiState.update { it.change().copy(errors = it.errors - field) }

    fun onPetChange(v: String) = edit(FIELD_PET) { copy(petId = v) }
    fun onKindChange(v: AppointmentKind) = edit(FIELD_KIND) { copy(kind = v) }
    fun onTitleChange(v: String) = edit(FIELD_TITLE) { copy(title = v) }
    fun onDateChange(v: String) = edit(FIELD_DATE) { copy(date = v) }
    fun onTimeChange(v: String) = edit(FIELD_TIME) { copy(time = v) }
    fun onVetNameChange(v: String) = edit(FIELD_VET) { copy(vetName = v) }
    fun onClinicChange(v: String) = edit(FIELD_CLINIC) { copy(clinic = v) }
    fun onNotesChange(v: String) = edit(FIELD_NOTES) { copy(notes = v) }
    fun onReminderChange(v: Int) = edit(FIELD_REMINDER) { copy(reminderDaysBefore = v.coerceIn(0, 30)) }
    fun onDoneChange(v: Boolean) = _uiState.update { it.copy(isDone = v) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving) return
        val errors = buildMap {
            if (s.petId.isBlank()) put(FIELD_PET, "กรุณาเลือกสัตว์เลี้ยง")
            if (s.title.isBlank()) put(FIELD_TITLE, "กรุณากรอกหัวข้อ")
            if (DateUtils.parseDate(s.date) == null) put(FIELD_DATE, "กรุณาเลือกวันที่")
            if (s.time.isNotBlank() && DateUtils.parseTime(s.time) == null) put(FIELD_TIME, "เวลาไม่ถูกต้อง")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        val appointment = (original ?: VetAppointment()).copy(
            petId = s.petId,
            kind = s.kind.name,
            title = s.title.trim(),
            date = s.date,
            time = s.time,
            vetName = s.vetName.trim(),
            clinic = s.clinic.trim(),
            notes = s.notes.trim(),
            reminderDaysBefore = s.reminderDaysBefore,
            status = if (s.isDone) AppointmentStatus.DONE.name else AppointmentStatus.PENDING.name,
        )
        val petName = s.pets.firstOrNull { it.id == s.petId }?.name.orEmpty()
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { appointmentRepository.save(appointment, petName) }
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = "บันทึกไม่สำเร็จ: ${e.message}") } }
        }
    }

    /** ชื่อหัวข้อแนะนำตามประเภท (ใช้เป็น placeholder) */
    fun suggestedTitle(kind: AppointmentKind): String = when (kind) {
        AppointmentKind.VACCINE -> "เช่น วัคซีนรวม, พิษสุนัขบ้า"
        AppointmentKind.CHECKUP -> "เช่น ตรวจสุขภาพประจำปี"
        AppointmentKind.TREATMENT -> "เช่น ติดตามอาการ, ทำแผล"
        AppointmentKind.MEDICATION -> "เช่น ถ่ายพยาธิ, ยากันเห็บหมัด"
        AppointmentKind.GROOMING -> "เช่น อาบน้ำ ตัดเล็บ"
        AppointmentKind.OTHER -> "หัวข้อนัดหมาย"
    }

    companion object {
        const val FIELD_PET = "pet"
        const val FIELD_KIND = "kind"
        const val FIELD_TITLE = "title"
        const val FIELD_DATE = "date"
        const val FIELD_TIME = "time"
        const val FIELD_VET = "vet"
        const val FIELD_CLINIC = "clinic"
        const val FIELD_NOTES = "notes"
        const val FIELD_REMINDER = "reminder"
    }
}
