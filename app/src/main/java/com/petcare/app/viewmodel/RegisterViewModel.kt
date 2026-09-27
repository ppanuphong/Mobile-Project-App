package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AuthRepository
import com.petcare.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registeredUser: User? = null,
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) = edit(FIELD_NAME) { copy(name = v) }
    fun onPhoneChange(v: String) = edit(FIELD_PHONE) { copy(phone = v.filter { it.isDigit() || it == '-' }) }
    fun onEmailChange(v: String) = edit(FIELD_EMAIL) { copy(email = v) }
    fun onPasswordChange(v: String) = edit(FIELD_PASSWORD) { copy(password = v) }
    fun onConfirmChange(v: String) = edit(FIELD_CONFIRM) { copy(confirmPassword = v) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    private fun edit(field: String, change: RegisterUiState.() -> RegisterUiState) =
        _uiState.update { it.change().copy(errors = it.errors - field, errorMessage = null) }

    fun register() {
        val s = _uiState.value
        if (s.isLoading) return
        val errors = buildMap {
            if (s.name.isBlank()) put(FIELD_NAME, "กรุณากรอกชื่อ")
            validateEmail(s.email.trim())?.let { put(FIELD_EMAIL, it) }
            if (s.password.length < 6) put(FIELD_PASSWORD, "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร")
            if (s.confirmPassword != s.password) put(FIELD_CONFIRM, "รหัสผ่านไม่ตรงกัน")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.register(s.name.trim(), s.phone.trim(), s.email.trim(), s.password)
                .onSuccess { user -> _uiState.update { it.copy(isLoading = false, registeredUser = user) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    companion object {
        const val FIELD_NAME = "name"
        const val FIELD_PHONE = "phone"
        const val FIELD_EMAIL = "email"
        const val FIELD_PASSWORD = "password"
        const val FIELD_CONFIRM = "confirm"
    }
}
