package com.petcare.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.withPets
import com.petcare.app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppointmentDetailUiState(
    val isLoading: Boolean = true,
    val item: AppointmentItem? = null,
    val deleted: Boolean = false,
)

/** รายละเอียดนัดหมาย 1 รายการ (อ่านอย่างเดียว) อัปเดตแบบ realtime หลังกลับมาจากหน้าแก้ไข */
class AppointmentDetailViewModel(
    savedStateHandle: SavedStateHandle,
    petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val appointmentId: String = checkNotNull(savedStateHandle[Routes.ARG_APPOINTMENT_ID])
    private val deleted = MutableStateFlow(false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<AppointmentDetailUiState> = combine(
        appointmentRepository.observeAppointment(appointmentId),
        petRepository.observePets(),
        deleted,
    ) { appointment, pets, isDeleted ->
        AppointmentDetailUiState(
            isLoading = false,
            item = appointment?.let { listOf(it).withPets(pets).first() },
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

    fun messageShown() {
        _message.value = null
    }
}
