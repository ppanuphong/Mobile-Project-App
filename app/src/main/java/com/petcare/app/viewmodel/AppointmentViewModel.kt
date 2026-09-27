package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AppointmentFilter
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.DisplayStatus
import com.petcare.app.model.Pet
import com.petcare.app.model.withPets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppointmentListUiState(
    val isLoading: Boolean = true,
    val filter: AppointmentFilter = AppointmentFilter.ALL,
    val petFilter: String? = null,
    val pets: List<Pet> = emptyList(),
    val items: List<AppointmentItem> = emptyList(),
    val counts: Map<AppointmentFilter, Int> = emptyMap(),
)

/** หน้ารายการนัดหมาย: กรองตามสถานะ/สัตว์เลี้ยง, ทำเครื่องหมายเสร็จ, ลบ */
class AppointmentViewModel(
    petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(AppointmentFilter.ALL)
    private val petFilter = MutableStateFlow<String?>(null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<AppointmentListUiState> = combine(
        petRepository.observePets(),
        appointmentRepository.observeAppointments(),
        filter,
        petFilter,
    ) { pets, appointments, selected, petId ->
        val all = appointments.withPets(pets).filter { petId == null || it.appointment.petId == petId }
        val filtered = all.filter { selected.matches(it.status) }
        AppointmentListUiState(
            isLoading = false,
            filter = selected,
            petFilter = petId,
            pets = pets,
            // เสร็จแล้ว: ล่าสุดก่อน, ที่เหลือ: ใกล้ที่สุดก่อน (เลยกำหนดขึ้นก่อนเสมอ)
            items = if (selected == AppointmentFilter.DONE) filtered.sortedByDescending { it.appointment.date }
            else filtered.sortedWith(compareBy({ it.status.sortOrder() }, { it.appointment.date }, { it.appointment.time })),
            counts = AppointmentFilter.entries.associateWith { f -> all.count { f.matches(it.status) } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppointmentListUiState())

    fun setFilter(value: AppointmentFilter) {
        filter.value = value
    }

    fun setPetFilter(petId: String?) {
        petFilter.value = petId
    }

    fun toggleDone(item: AppointmentItem) {
        viewModelScope.launch {
            val done = !item.appointment.isDone
            runCatching { appointmentRepository.setDone(item.appointment, done, item.pet?.name.orEmpty()) }
                .onSuccess { _message.value = if (done) "ทำเครื่องหมายว่าเสร็จแล้ว" else "ย้ายกลับไปรอดำเนินการ" }
                .onFailure { _message.value = "อัปเดตไม่สำเร็จ: ${it.message}" }
        }
    }

    fun delete(item: AppointmentItem) {
        viewModelScope.launch {
            runCatching { appointmentRepository.delete(item.appointment.id) }
                .onSuccess { _message.value = "ลบนัดหมายแล้ว" }
                .onFailure { _message.value = "ลบไม่สำเร็จ: ${it.message}" }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}

private fun DisplayStatus.sortOrder() = when (this) {
    DisplayStatus.OVERDUE -> 0
    DisplayStatus.DUE_SOON, DisplayStatus.UPCOMING -> 1
    DisplayStatus.DONE -> 2
}
