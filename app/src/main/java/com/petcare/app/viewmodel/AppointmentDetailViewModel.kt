package com.petcare.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.PetCondition
import com.petcare.app.model.ProgressUpdate
import com.petcare.app.model.VetAppointment
import com.petcare.app.model.withPets
import com.petcare.app.ui.navigation.Routes
import com.petcare.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppointmentDetailUiState(
    val isLoading: Boolean = true,
    val item: AppointmentItem? = null,
    /** นัดติดตามผลที่สร้างจากผลการรักษาของนัดนี้ */
    val followUp: AppointmentItem? = null,
    /** นัดเดิม ถ้านัดนี้เป็นนัดติดตามผล */
    val parent: AppointmentItem? = null,
    /** ไทม์ไลน์อาการ ล่าสุดก่อน */
    val progress: List<ProgressUpdate> = emptyList(),
    val deleted: Boolean = false,
)

/** รายละเอียดนัด 1 รายการ + ผลการรักษา + ไทม์ไลน์ติดตามอาการ */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentDetailViewModel(
    savedStateHandle: SavedStateHandle,
    petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val appointmentId: String = checkNotNull(savedStateHandle[Routes.ARG_APPOINTMENT_ID])
    private val deleted = MutableStateFlow(false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val appointment = appointmentRepository.observeAppointment(appointmentId)

    private fun observeOptional(id: String?): Flow<VetAppointment?> =
        if (id.isNullOrBlank()) flowOf(null) else appointmentRepository.observeAppointment(id)

    val uiState: StateFlow<AppointmentDetailUiState> = combine(
        appointment,
        appointment.flatMapLatest { observeOptional(it?.result?.followUpAppointmentId) },
        appointment.flatMapLatest { observeOptional(it?.followUpOf) },
        petRepository.observePets(),
        deleted,
    ) { appt, followUp, parent, pets, isDeleted ->
        AppointmentDetailUiState(
            isLoading = false,
            item = appt?.let { listOf(it).withPets(pets).first() },
            followUp = followUp?.let { listOf(it).withPets(pets).first() },
            parent = parent?.let { listOf(it).withPets(pets).first() },
            progress = appt?.progress.orEmpty().sortedWith(compareByDescending<ProgressUpdate> { it.date }.thenByDescending { it.id }),
            deleted = isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppointmentDetailUiState())

    fun toggleDone() {
        val item = uiState.value.item ?: return
        viewModelScope.launch {
            runCatching {
                appointmentRepository.setDone(item.appointment, !item.appointment.isDone, item.pet?.name.orEmpty())
            }.onFailure { _message.value = "อัปเดตไม่สำเร็จ: ${it.message}" }
        }
    }

    fun delete() {
        viewModelScope.launch {
            runCatching { appointmentRepository.delete(appointmentId) }
                .onSuccess { deleted.value = true }
                .onFailure { _message.value = "ลบไม่สำเร็จ: ${it.message}" }
        }
    }

    fun addProgress(date: String, condition: PetCondition, note: String) {
        val appt = uiState.value.item?.appointment ?: return
        if (DateUtils.parseDate(date)?.isAfter(LocalDate.now()) == true) {
            _message.value = "วันที่ต้องไม่เป็นวันในอนาคต"
            return
        }
        viewModelScope.launch {
            runCatching {
                appointmentRepository.addProgress(appt, ProgressUpdate(date = date, condition = condition.name, note = note.trim()))
            }
                .onSuccess {
                    _message.value = if (condition == PetCondition.RECOVERED) "ปิดเคส: หายดีแล้ว 🎉" else "บันทึกอาการแล้ว"
                }
                .onFailure { _message.value = "บันทึกไม่สำเร็จ: ${it.message}" }
        }
    }

    fun deleteProgress(update: ProgressUpdate) {
        val appt = uiState.value.item?.appointment ?: return
        viewModelScope.launch {
            runCatching { appointmentRepository.deleteProgress(appt, update.id) }
                .onFailure { _message.value = "ลบไม่สำเร็จ: ${it.message}" }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
