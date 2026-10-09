package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.data.SessionManager
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.DisplayStatus
import com.petcare.app.model.Pet
import com.petcare.app.model.withPets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PetSummary(val pet: Pet, val nextAppointment: AppointmentItem?, val overdueCount: Int)

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val overdue: List<AppointmentItem> = emptyList(),
    val dueSoon: List<AppointmentItem> = emptyList(),
    /** เคสที่ยังอยู่ระหว่างรักษา ต้องติดตามอาการ */
    val underTreatment: List<AppointmentItem> = emptyList(),
    val pets: List<PetSummary> = emptyList(),
    val doneThisMonth: Int = 0,
)

class HomeViewModel(
    petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        petRepository.observePets(),
        appointmentRepository.observeAppointments(),
        sessionManager.session,
    ) { pets, appointments, session ->
        val items = appointments.withPets(pets)
        val pending = items.filter { it.status != DisplayStatus.DONE }
        val monthPrefix = LocalDate.now().toString().take(7) // "yyyy-MM"
        HomeUiState(
            isLoading = false,
            userName = session?.name.orEmpty(),
            overdue = pending.filter { it.status == DisplayStatus.OVERDUE }.sortedByDescending { it.appointment.date },
            dueSoon = pending.filter { it.status == DisplayStatus.DUE_SOON },
            underTreatment = items.filter { it.appointment.isUnderTreatment }.sortedByDescending { it.appointment.date },
            pets = pets.map { pet ->
                val mine = pending.filter { it.appointment.petId == pet.id }
                PetSummary(
                    pet = pet,
                    nextAppointment = mine.firstOrNull { it.status != DisplayStatus.OVERDUE },
                    overdueCount = mine.count { it.status == DisplayStatus.OVERDUE },
                )
            },
            doneThisMonth = items.count { it.status == DisplayStatus.DONE && it.appointment.date.startsWith(monthPrefix) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun markDone(item: AppointmentItem) {
        viewModelScope.launch {
            runCatching { appointmentRepository.setDone(item.appointment, true, item.pet?.name.orEmpty()) }
        }
    }
}
