package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.AuthRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.data.SessionManager
import com.petcare.app.data.UserRepository
import com.petcare.app.model.Pet
import com.petcare.app.model.User
import com.petcare.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User = User(),
    /** "ตุลาคม 2569" — วันที่สมัครสมาชิก */
    val memberSince: String? = null,
    val pets: List<Pet> = emptyList(),
    val appointmentCount: Int = 0,
    val doneCount: Int = 0,
    val underTreatmentCount: Int = 0,
    /** นัดที่ถึงช่วงเตือนหรือเลยกำหนด แสดงเป็นตัวเลขที่เมนู "การแจ้งเตือน" */
    val attentionCount: Int = 0,
)

/** หน้าโปรไฟล์ผู้ใช้ (แบบ Facebook): รูปหน้าปก รูปโปรไฟล์ ข้อมูลแนะนำตัว สัตว์เลี้ยง และเมนูตั้งค่า */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    userRepository: UserRepository,
    petRepository: PetRepository,
    appointmentRepository: AppointmentRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ถ้าเอกสารผู้ใช้ยังโหลดไม่ได้ (เช่น ออฟไลน์ครั้งแรก) ใช้ชื่อ/อีเมลจาก session ไปก่อน
    private val user: Flow<User> = combine(
        authRepository.currentUserId?.let { userRepository.observeUser(it) } ?: flowOf(null),
        sessionManager.session,
    ) { doc, session ->
        doc ?: User(id = session?.userId.orEmpty(), name = session?.name.orEmpty(), email = session?.email.orEmpty())
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        user,
        petRepository.observePets(),
        appointmentRepository.observeAppointments(),
    ) { user, pets, appointments ->
        ProfileUiState(
            isLoading = false,
            user = user,
            memberSince = authRepository.memberSinceMillis?.let(DateUtils::thaiMonthYear),
            pets = pets,
            appointmentCount = appointments.size,
            doneCount = appointments.count { it.isDone },
            underTreatmentCount = appointments.count { it.isUnderTreatment },
            attentionCount = appointments.count { it.isInReminderWindow() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun sendPasswordReset() {
        val email = uiState.value.user.email
        if (email.isBlank()) {
            _message.value = "ไม่พบอีเมลของบัญชีนี้"
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess { _message.value = "ส่งลิงก์ตั้งรหัสผ่านใหม่ไปที่ $email แล้ว" }
                .onFailure { _message.value = it.message }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
