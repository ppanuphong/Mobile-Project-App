package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.model.DisplayStatus
import com.petcare.app.model.Pet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PetListItem(val pet: Pet, val pendingCount: Int, val overdueCount: Int)

data class PetListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val pets: List<PetListItem> = emptyList(),
)

/** หน้ารายชื่อสัตว์เลี้ยง: ค้นหา + ลบ */
class PetViewModel(
    private val petRepository: PetRepository,
    appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<PetListUiState> = combine(
        petRepository.observePets(),
        appointmentRepository.observeAppointments(),
        query,
    ) { pets, appointments, q ->
        val filtered = if (q.isBlank()) pets else pets.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.species.contains(q, ignoreCase = true) ||
                it.breed.contains(q, ignoreCase = true)
        }
        PetListUiState(
            isLoading = false,
            query = q,
            pets = filtered.map { pet ->
                val mine = appointments.filter { it.petId == pet.id && !it.isDone }
                PetListItem(
                    pet = pet,
                    pendingCount = mine.size,
                    overdueCount = mine.count { it.displayStatus() == DisplayStatus.OVERDUE },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetListUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun deletePet(pet: Pet) {
        viewModelScope.launch {
            _message.value = runCatching { petRepository.deletePet(pet.id) }
                .fold({ "ลบ ${pet.name} แล้ว" }, { "ลบไม่สำเร็จ: ${it.message}" })
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
