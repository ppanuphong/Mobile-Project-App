package com.petcare.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AuthRepository
import com.petcare.app.data.PetPhotoProcessor
import com.petcare.app.data.SessionManager
import com.petcare.app.data.UserRepository
import com.petcare.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val name: String = "",
    val bio: String = "",
    val phone: String = "",
    val location: String = "",
    val photo: String = "",
    val coverPhoto: String = "",
    val processingPhoto: Boolean = false,
    val processingCover: Boolean = false,
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

/** แก้ไขโปรไฟล์: รูปโปรไฟล์ รูปหน้าปก ชื่อ แนะนำตัว เบอร์โทร ที่อยู่ (อีเมลแก้ไม่ได้ เพราะใช้เข้าสู่ระบบ) */
class ProfileEditViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val photoProcessor: PetPhotoProcessor,
) : ViewModel() {

    private var original: User? = null

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = authRepository.currentUserId
            val session = sessionManager.currentSession()
            val user = uid?.let { runCatching { userRepository.getUser(it) }.getOrNull() }
                ?: User(id = uid ?: session?.userId.orEmpty(), name = session?.name.orEmpty(), email = session?.email.orEmpty())
            original = user
            _uiState.update {
                it.copy(
                    isLoading = false,
                    email = user.email,
                    name = user.name,
                    bio = user.bio,
                    phone = user.phone,
                    location = user.location,
                    photo = user.photo,
                    coverPhoto = user.coverPhoto,
                )
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun onBioChange(v: String) = _uiState.update { it.copy(bio = v.take(BIO_MAX)) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v.filter { c -> c.isDigit() || c == '-' }) }
    fun onLocationChange(v: String) = _uiState.update { it.copy(location = v) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun onPhotoPicked(uri: Uri) {
        _uiState.update { it.copy(processingPhoto = true) }
        viewModelScope.launch {
            runCatching { photoProcessor.encode(uri) }
                .onSuccess { photo -> _uiState.update { it.copy(processingPhoto = false, photo = photo) } }
                .onFailure { _uiState.update { it.copy(processingPhoto = false, errorMessage = PHOTO_ERROR) } }
        }
    }

    fun onCoverPicked(uri: Uri) {
        _uiState.update { it.copy(processingCover = true) }
        viewModelScope.launch {
            runCatching { photoProcessor.encode(uri, PetPhotoProcessor.COVER_WIDTH, PetPhotoProcessor.COVER_HEIGHT) }
                .onSuccess { cover -> _uiState.update { it.copy(processingCover = false, coverPhoto = cover) } }
                .onFailure { _uiState.update { it.copy(processingCover = false, errorMessage = PHOTO_ERROR) } }
        }
    }

    fun onRemovePhoto() = _uiState.update { it.copy(photo = "") }
    fun onRemoveCover() = _uiState.update { it.copy(coverPhoto = "") }

    fun save() {
        val s = _uiState.value
        val base = original ?: return
        if (s.isSaving || s.processingPhoto || s.processingCover) return
        if (s.name.isBlank()) {
            _uiState.update { it.copy(nameError = "กรุณากรอกชื่อ") }
            return
        }
        val updated = base.copy(
            name = s.name.trim(),
            bio = s.bio.trim(),
            phone = s.phone.trim(),
            location = s.location.trim(),
            photo = s.photo,
            coverPhoto = s.coverPhoto,
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                userRepository.saveUser(updated)
                sessionManager.save(updated) // ชื่อในหน้าหลักใช้ค่าจาก session
            }
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = "บันทึกไม่สำเร็จ: ${e.message}") } }
        }
    }

    companion object {
        /** ความยาวคำแนะนำตัวสูงสุด (เท่ากับ Facebook) */
        const val BIO_MAX = 101
    }
}
