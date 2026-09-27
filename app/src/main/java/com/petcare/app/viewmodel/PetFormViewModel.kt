package com.petcare.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AVATAR_COLORS
import com.petcare.app.model.Pet
import com.petcare.app.ui.navigation.Routes
import com.petcare.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PetFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val species: String = "สุนัข",
    val breed: String = "",
    val birthday: String = "",
    val avatarColor: Long = AVATAR_COLORS.first(),
    val nameError: String? = null,
    val speciesError: String? = null,
    val birthdayError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedId: String? = null,
)

/** ฟอร์มเพิ่ม / แก้ไขสัตว์เลี้ยง (ถ้ามี petId ใน route = แก้ไข) */
class PetFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val petRepository: PetRepository,
) : ViewModel() {

    private val petId: String? = savedStateHandle.get<String>(Routes.ARG_PET_ID)?.takeIf { it.isNotBlank() }
    private var original: Pet? = null

    private val _uiState = MutableStateFlow(PetFormUiState(isEdit = petId != null, isLoading = petId != null))
    val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

    init {
        if (petId != null) {
            viewModelScope.launch {
                val pet = petRepository.observePet(petId).firstOrNull()
                original = pet
                _uiState.update {
                    if (pet == null) it.copy(isLoading = false, errorMessage = "ไม่พบข้อมูลสัตว์เลี้ยง")
                    else it.copy(
                        isLoading = false,
                        name = pet.name,
                        species = pet.species,
                        breed = pet.breed,
                        birthday = pet.birthday,
                        avatarColor = pet.avatarColor,
                    )
                }
            }
        } else {
            _uiState.update { it.copy(avatarColor = AVATAR_COLORS.random()) }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun onSpeciesChange(v: String) = _uiState.update { it.copy(species = v, speciesError = null) }
    fun onBreedChange(v: String) = _uiState.update { it.copy(breed = v) }
    fun onBirthdayChange(v: String) = _uiState.update { it.copy(birthday = v, birthdayError = null) }
    fun onColorChange(v: Long) = _uiState.update { it.copy(avatarColor = v) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving) return
        val nameError = if (s.name.isBlank()) "กรุณากรอกชื่อ" else null
        val speciesError = if (s.species.isBlank()) "กรุณาระบุชนิดสัตว์" else null
        val birthdayError = DateUtils.parseDate(s.birthday)
            ?.takeIf { it.isAfter(LocalDate.now()) }
            ?.let { "วันเกิดต้องไม่เป็นวันในอนาคต" }
        if (nameError != null || speciesError != null || birthdayError != null) {
            _uiState.update { it.copy(nameError = nameError, speciesError = speciesError, birthdayError = birthdayError) }
            return
        }

        val pet = (original ?: Pet()).copy(
            name = s.name.trim(),
            species = s.species.trim(),
            breed = s.breed.trim(),
            birthday = s.birthday,
            avatarColor = s.avatarColor,
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { petRepository.savePet(pet) }
                .onSuccess { id -> _uiState.update { it.copy(isSaving = false, savedId = id) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = "บันทึกไม่สำเร็จ: ${e.message}") } }
        }
    }
}
