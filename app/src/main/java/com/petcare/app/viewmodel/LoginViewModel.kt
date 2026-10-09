package com.petcare.app.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AuthRepository
import com.petcare.app.model.User
import com.petcare.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: User? = null,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun login() {
        val state = _uiState.value
        val email = state.email.trim()
        val emailError = validateEmail(email)
        val passwordError = if (state.password.isBlank()) "กรุณากรอกรหัสผ่าน" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        authenticate { authRepository.login(email, state.password) }
    }

    fun loginDemo() = authenticate { authRepository.loginDemo() }

    private fun authenticate(call: suspend () -> Result<User>) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            call()
                .onSuccess { user -> _uiState.update { it.copy(isLoading = false, loggedInUser = user) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }
}

internal const val PHOTO_ERROR = "ใช้รูปนี้ไม่ได้ ลองเลือกรูปอื่น"

/** วันเกิดสัตว์เลี้ยงเว้นว่างได้ แต่ต้องไม่ใช่วันในอนาคต */
internal fun validatePetBirthday(birthday: String): String? =
    DateUtils.parseDate(birthday)?.takeIf { it.isAfter(LocalDate.now()) }?.let { "วันเกิดต้องไม่เป็นวันในอนาคต" }

internal fun validateEmail(email: String): String? = when {
    email.isBlank() -> "กรุณากรอกอีเมล"
    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "รูปแบบอีเมลไม่ถูกต้อง"
    else -> null
}
