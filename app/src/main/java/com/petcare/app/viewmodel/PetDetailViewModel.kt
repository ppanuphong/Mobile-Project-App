package com.petcare.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.AppointmentKind
import com.petcare.app.model.DisplayStatus
import com.petcare.app.model.Pet
import com.petcare.app.model.latestWeightKg
import com.petcare.app.model.withPets
import com.petcare.app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PetHistoryTab(val label: String) { UPCOMING("กำลังจะถึง"), VACCINES("ประวัติวัคซีน"), VISITS("ประวัติหาหมอ") }

data class PetDetailUiState(
    val isLoading: Boolean = true,
    val pet: Pet? = null,
    val tab: PetHistoryTab = PetHistoryTab.UPCOMING,
    val upcoming: List<AppointmentItem> = emptyList(),
    val vaccineHistory: List<AppointmentItem> = emptyList(),
    val visitHistory: List<AppointmentItem> = emptyList(),
    val latestWeightKg: Double? = null,
    val deleted: Boolean = false,
)

/** โปรไฟล์สัตว์เลี้ยง 1 ตัว พร้อมประวัติวัคซีน / นัดหมอ แยกรายตัว */
class PetDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val petId: String = checkNotNull(savedStateHandle[Routes.ARG_PET_ID])
    private val tab = MutableStateFlow(PetHistoryTab.UPCOMING)
    private val deleted = MutableStateFlow(false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<PetDetailUiState> = combine(
        petRepository.observePet(petId),
        appointmentRepository.observeByPet(petId),
        tab,
        deleted,
    ) { pet, appointments, selectedTab, isDeleted ->
        val items = appointments.withPets(listOfNotNull(pet))
        val done = items.filter { it.status == DisplayStatus.DONE }.sortedByDescending { it.appointment.date }
        val vaccines = done.filter { it.appointment.kindEnum == AppointmentKind.VACCINE }
        val visits = done.filter { it.appointment.kindEnum != AppointmentKind.VACCINE }
        PetDetailUiState(
            isLoading = false,
            pet = pet,
            tab = selectedTab,
            upcoming = items.filter { it.status != DisplayStatus.DONE },
            vaccineHistory = vaccines,
            visitHistory = visits,
            latestWeightKg = appointments.latestWeightKg(),
            deleted = isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetDetailUiState())

    fun selectTab(value: PetHistoryTab) {
        tab.value = value
    }

    fun toggleDone(item: AppointmentItem) {
        viewModelScope.launch {
            runCatching {
                appointmentRepository.setDone(item.appointment, !item.appointment.isDone, item.pet?.name.orEmpty())
            }.onFailure { _message.value = "อัปเดตไม่สำเร็จ: ${it.message}" }
        }
    }

    fun deletePet() {
        viewModelScope.launch {
            runCatching { petRepository.deletePet(petId) }
                .onSuccess { deleted.value = true }
                .onFailure { _message.value = "ลบไม่สำเร็จ: ${it.message}" }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
