package com.petcare.app.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AuthRepository
import com.petcare.app.data.PetCatalogRepository
import com.petcare.app.data.PetPhotoProcessor
import com.petcare.app.data.PetRepository
import com.petcare.app.model.AVATAR_COLORS
import com.petcare.app.model.Breed
import com.petcare.app.model.Pet
import com.petcare.app.model.PetCategory
import com.petcare.app.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ส่วน "เลือกสัตว์เลี้ยง" ในหน้าสมัครสมาชิก (ไม่บังคับ) */
data class PetPickerState(
    val categoriesLoading: Boolean = true,
    val categoriesError: String? = null,
    val categories: List<PetCategory> = emptyList(),
    val selectedCategory: PetCategory? = null,
    val breedsLoading: Boolean = false,
    val breedsError: String? = null,
    val breeds: List<Breed> = emptyList(),
    val breedQuery: String = "",
    val selectedBreed: Breed? = null,
    val breedImageUrl: String? = null,
    val petName: String = "",
    val birthday: String = "",
    val avatarColor: Long = AVATAR_COLORS.first(),
    val photo: String = "",
    val isProcessingPhoto: Boolean = false,
) {
    /** สายพันธุ์ที่ตรงกับคำค้น (ค้นทั้งชื่อและคำอธิบาย) */
    val visibleBreeds: List<Breed>
        get() = if (breedQuery.isBlank()) breeds else breeds.filter {
            it.name.contains(breedQuery, ignoreCase = true) || it.subtitle.contains(breedQuery, ignoreCase = true)
        }
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
    private val petCatalogRepository: PetCatalogRepository,
    private val photoProcessor: PetPhotoProcessor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var breedsJob: Job? = null
    private var imageJob: Job? = null

    init {
        loadCategories()
    }

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

    // ---------- เลือกสัตว์เลี้ยง ----------

    fun loadCategories() {
        updatePet { copy(categoriesLoading = true, categoriesError = null) }
        viewModelScope.launch {
            runCatching { petCatalogRepository.getCategories() }
                .onSuccess { list -> updatePet { copy(categoriesLoading = false, categories = list) } }
                .onFailure { e ->
                    Log.w(TAG, "load categories failed", e)
                    updatePet { copy(categoriesLoading = false, categoriesError = API_ERROR) }
                }
        }
    }

    /** เลือกหมวดหมู่ (กดซ้ำ = ยกเลิก) แล้วโหลดสายพันธุ์ของหมวดนั้น */
    fun selectCategory(category: PetCategory) {
        if (_uiState.value.pet.selectedCategory?.id == category.id) {
            clearPet()
            return
        }
        imageJob?.cancel()
        updatePet {
            copy(
                selectedCategory = category,
                avatarColor = if (selectedCategory == null) AVATAR_COLORS.random() else avatarColor,
                breeds = emptyList(),
                breedQuery = "",
                selectedBreed = null,
                breedImageUrl = null,
            )
        }
        loadBreeds()
    }

    fun loadBreeds() {
        val category = _uiState.value.pet.selectedCategory ?: return
        breedsJob?.cancel()
        updatePet { copy(breedsLoading = true, breedsError = null) }
        breedsJob = viewModelScope.launch {
            runCatching { petCatalogRepository.getBreeds(category) }
                .onSuccess { list -> updatePet { copy(breedsLoading = false, breeds = list) } }
                .onFailure { e ->
                    Log.w(TAG, "load breeds failed", e)
                    updatePet { copy(breedsLoading = false, breedsError = API_ERROR) }
                }
        }
    }

    fun onBreedQueryChange(v: String) = updatePet { copy(breedQuery = v) }

    fun selectBreed(breed: Breed) {
        updatePet { copy(selectedBreed = breed, breedImageUrl = null, breedQuery = "") }
        imageJob?.cancel()
        if (breed.dogCeoPath != null) {
            imageJob = viewModelScope.launch {
                // รูปเป็นแค่ส่วนเสริม โหลดไม่ได้ก็ไม่ต้องแจ้ง
                val url = runCatching { petCatalogRepository.getBreedImage(breed) }.getOrNull()
                updatePet { if (selectedBreed?.id == breed.id) copy(breedImageUrl = url) else this }
            }
        }
    }

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
        imageJob?.cancel()
        _uiState.update {
            it.copy(
                pet = it.pet.copy(
                    selectedCategory = null,
                    breeds = emptyList(),
                    breedsLoading = false,
                    breedsError = null,
                    breedQuery = "",
                    selectedBreed = null,
                    breedImageUrl = null,
                    petName = "",
                    birthday = "",
                    photo = "",
                ),
                errors = it.errors - FIELD_PET_NAME - FIELD_PET_BIRTHDAY,
            )
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
            if (s.pet.selectedCategory != null && s.pet.petName.isBlank()) {
                put(FIELD_PET_NAME, "กรุณาตั้งชื่อสัตว์เลี้ยง หรือกด \"ไม่เพิ่มตอนนี้\"")
            }
            if (s.pet.selectedCategory != null) {
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
        val category = picker.selectedCategory ?: return null
        val breed = picker.selectedBreed?.takeIf { it != Breed.UNKNOWN }
        return Pet(
            name = picker.petName.trim(),
            species = breed?.species ?: category.name,
            breed = breed?.name.orEmpty(),
            birthday = picker.birthday,
            avatarColor = picker.avatarColor,
            photo = picker.photo,
        )
    }

    companion object {
        private const val TAG = "RegisterViewModel"
        private const val API_ERROR = "โหลดข้อมูลจาก API ไม่สำเร็จ ตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"

        const val FIELD_NAME = "name"
        const val FIELD_PHONE = "phone"
        const val FIELD_EMAIL = "email"
        const val FIELD_PASSWORD = "password"
        const val FIELD_CONFIRM = "confirm"
        const val FIELD_PET_NAME = "pet_name"
        const val FIELD_PET_BIRTHDAY = "pet_birthday"
    }
}
