package com.petcare.app.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AuthRepository
import com.petcare.app.data.BreedRepository
import com.petcare.app.data.PetPhotoProcessor
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AVATAR_COLORS
import com.petcare.app.model.Breed
import com.petcare.app.model.BreedGroup
import com.petcare.app.model.Pet
import com.petcare.app.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ส่วน "สัตว์เลี้ยงของคุณ" ในหน้าสมัครสมาชิก (ไม่บังคับ) ใช้ช่องแบบเดียวกับหน้าเพิ่มสัตว์เลี้ยง
 * species ว่าง = ไม่เพิ่มสัตว์เลี้ยงตอนสมัคร
 */
data class PetPickerState(
    val species: String = "",
    val breed: String = "",
    val breedOptions: List<Breed> = emptyList(),
    val breedsLoading: Boolean = false,
    /** false = ใช้รายการพื้นฐานในเครื่อง (ออฟไลน์) */
    val breedsFromApi: Boolean = false,
    val petName: String = "",
    val birthday: String = "",
    val avatarColor: Long = AVATAR_COLORS.first(),
    val photo: String = "",
    val isProcessingPhoto: Boolean = false,
) {
    val isAddingPet: Boolean
        get() = species.isNotBlank()
}

data class RegisterUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val pet: PetPickerState = PetPickerState(),
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registeredUser: User? = null,
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val breedRepository: BreedRepository,
    private val photoProcessor: PetPhotoProcessor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var breedsJob: Job? = null

    fun onNameChange(v: String) = edit(FIELD_NAME) { copy(name = v) }
    fun onPhoneChange(v: String) = edit(FIELD_PHONE) { copy(phone = v.filter { it.isDigit() || it == '-' }) }
    fun onEmailChange(v: String) = edit(FIELD_EMAIL) { copy(email = v) }
    fun onPasswordChange(v: String) = edit(FIELD_PASSWORD) { copy(password = v) }
    fun onConfirmChange(v: String) = edit(FIELD_CONFIRM) { copy(confirmPassword = v) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    private fun edit(field: String, change: RegisterUiState.() -> RegisterUiState) =
        _uiState.update { it.change().copy(errors = it.errors - field, errorMessage = null) }

    private fun updatePet(change: PetPickerState.() -> PetPickerState) =
        _uiState.update { it.copy(pet = it.pet.change()) }

    // ---------- เลือกสัตว์เลี้ยง (เหมือนหน้าเพิ่มสัตว์เลี้ยง) ----------

    /** เลือกชนิดสัตว์ แล้วโหลดรายชื่อพันธุ์ของชนิดนั้น (ล้างพันธุ์เดิมถ้ากลุ่มเปลี่ยน) */
    fun onSpeciesChange(v: String) {
        val current = _uiState.value.pet
        val groupChanged = BreedGroup.fromSpecies(v) != BreedGroup.fromSpecies(current.species)
        updatePet {
            copy(
                species = v,
                breed = if (groupChanged) "" else breed,
                // สุ่มสีประจำตัวให้ตอนเริ่มเพิ่มสัตว์เลี้ยง
                avatarColor = if (!isAddingPet) AVATAR_COLORS.random() else avatarColor,
            )
        }
        if (groupChanged) loadBreeds(v)
    }

    private fun loadBreeds(species: String) {
        breedsJob?.cancel()
        updatePet { copy(breedsLoading = true) }
        breedsJob = viewModelScope.launch {
            val result = breedRepository.breedsFor(species)
            updatePet { copy(breedOptions = result.breeds, breedsFromApi = result.fromApi, breedsLoading = false) }
        }
    }

    fun onBreedChange(v: String) = updatePet { copy(breed = v) }

    fun onPetNameChange(v: String) = _uiState.update {
        it.copy(pet = it.pet.copy(petName = v), errors = it.errors - FIELD_PET_NAME)
    }

    fun onBirthdayChange(v: String) = _uiState.update {
        it.copy(pet = it.pet.copy(birthday = v), errors = it.errors - FIELD_PET_BIRTHDAY)
    }

    fun onAvatarColorChange(v: Long) = updatePet { copy(avatarColor = v) }

    fun onPhotoPicked(uri: Uri) {
        updatePet { copy(isProcessingPhoto = true) }
        viewModelScope.launch {
            runCatching { photoProcessor.encode(uri) }
                .onSuccess { photo -> updatePet { copy(isProcessingPhoto = false, photo = photo) } }
                .onFailure { e ->
                    Log.w(TAG, "encode photo failed", e)
                    updatePet { copy(isProcessingPhoto = false) }
                    _uiState.update { it.copy(errorMessage = PHOTO_ERROR) }
                }
        }
    }

    fun onRemovePhoto() = updatePet { copy(photo = "") }

    /** ไม่เพิ่มสัตว์เลี้ยงตอนสมัคร */
    fun clearPet() {
        breedsJob?.cancel()
        _uiState.update {
            it.copy(pet = PetPickerState(), errors = it.errors - FIELD_PET_NAME - FIELD_PET_BIRTHDAY)
        }
    }

    // ---------- สมัครสมาชิก ----------

    fun register() {
        val s = _uiState.value
        if (s.isLoading || s.pet.isProcessingPhoto) return
        val errors = buildMap {
            if (s.name.isBlank()) put(FIELD_NAME, "กรุณากรอกชื่อ")
            validateEmail(s.email.trim())?.let { put(FIELD_EMAIL, it) }
            if (s.password.length < 6) put(FIELD_PASSWORD, "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร")
            if (s.confirmPassword != s.password) put(FIELD_CONFIRM, "รหัสผ่านไม่ตรงกัน")
            if (s.pet.isAddingPet && s.pet.petName.isBlank()) {
                put(FIELD_PET_NAME, "กรุณาตั้งชื่อสัตว์เลี้ยง หรือกด \"ไม่เพิ่มตอนนี้\"")
            }
            if (s.pet.isAddingPet) {
                validatePetBirthday(s.pet.birthday)?.let { put(FIELD_PET_BIRTHDAY, it) }
            }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.register(s.name.trim(), s.phone.trim(), s.email.trim(), s.password)
                .onSuccess { user ->
                    firstPetOf(s.pet)?.let { pet ->
                        // สมัครสำเร็จแล้ว ถ้าบันทึกสัตว์เลี้ยงไม่ได้ ผู้ใช้ยังเพิ่มเองภายหลังได้
                        runCatching { petRepository.savePet(pet) }
                            .onFailure { e -> Log.w(TAG, "save first pet failed", e) }
                    }
                    _uiState.update { it.copy(isLoading = false, registeredUser = user) }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    private fun firstPetOf(picker: PetPickerState): Pet? {
        if (!picker.isAddingPet) return null
        return Pet(
            name = picker.petName.trim(),
            species = picker.species,
            breed = picker.breed.trim(),
            birthday = picker.birthday,
            avatarColor = picker.avatarColor,
            photo = picker.photo,
        )
    }

    companion object {
        private const val TAG = "RegisterViewModel"

        const val FIELD_NAME = "name"
        const val FIELD_PHONE = "phone"
        const val FIELD_EMAIL = "email"
        const val FIELD_PASSWORD = "password"
        const val FIELD_CONFIRM = "confirm"
        const val FIELD_PET_NAME = "pet_name"
        const val FIELD_PET_BIRTHDAY = "pet_birthday"
    }
}
