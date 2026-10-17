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
    /** ข้อความแจ้งผลที่ไม่ใช่ error เช่น ส่งลิงก์รีเซ็ตรหัสผ่านแล้ว */
    val infoMessage: String? = null,
    val loggedInUser: User? = null,
    // ---- ลืมรหัสผ่าน ----
    val resetSending: Boolean = false,
    val resetError: String? = null,
    /** ส่งลิงก์สำเร็จ ให้ UI ปิดหน้าต่างลืมรหัสผ่าน */
    val resetSent: Boolean = false,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null, infoMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null, infoMessage = null) }

    /** ลืมรหัสผ่าน: ส่งลิงก์ตั้งรหัสผ่านใหม่ไปที่อีเมลผ่าน Firebase Authentication */
    fun sendPasswordReset(email: String) {
        val trimmed = email.trim()
        validateEmail(trimmed)?.let { error ->
            _uiState.update { it.copy(resetError = error) }
            return
        }
        if (_uiState.value.resetSending) return
        _uiState.update { it.copy(resetSending = true, resetError = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(trimmed)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            resetSending = false,
                            resetSent = true,
                            email = trimmed,
                            errorMessage = null,
                            // Firebase ไม่บอกว่าอีเมลนี้มีบัญชีหรือไม่ (กันการเดาอีเมล) จึงใช้ข้อความกลาง ๆ
                            infoMessage = "ถ้า $trimmed มีบัญชีอยู่ ระบบได้ส่งลิงก์ตั้งรหัสผ่านใหม่ไปแล้ว " +
                                "กรุณาตรวจสอบกล่องจดหมาย (รวมถึงจดหมายขยะ)",
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(resetSending = false, resetError = e.message) } }
        }
    }

    /** ปิดหน้าต่างลืมรหัสผ่าน (ล้าง error และสถานะส่งแล้ว) */
    fun onResetDialogClosed() = _uiState.update { it.copy(resetError = null, resetSent = false) }

    fun clearResetError() = _uiState.update { it.copy(resetError = null) }

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
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
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
