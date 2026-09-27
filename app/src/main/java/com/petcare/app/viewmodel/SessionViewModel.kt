package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.AuthRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.notification.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class AuthState { CHECKING, LOGGED_IN, LOGGED_OUT }

/**
 * ViewModel ระดับ Activity: ตัดสินว่าเปิดแอปแล้วไปหน้า Login หรือ Home
 * และซิงก์การแจ้งเตือนกับข้อมูล realtime ขณะที่ผู้ใช้ล็อกอินอยู่
 */
class SessionViewModel(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.CHECKING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _attentionCount = MutableStateFlow(0)
    /** จำนวนนัดที่ถึงช่วงเตือนหรือเลยกำหนด ใช้แสดง badge บนแท็บแจ้งเตือน */
    val attentionCount: StateFlow<Int> = _attentionCount.asStateFlow()

    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            if (authRepository.hasValidSession()) onLoggedIn() else _authState.value = AuthState.LOGGED_OUT
        }
    }

    fun onLoggedIn() {
        _authState.value = AuthState.LOGGED_IN
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            combine(petRepository.observePets(), appointmentRepository.observeAppointments()) { pets, appts ->
                pets.associate { it.id to it.name } to appts
            }.collect { (petNames, appts) ->
                reminderScheduler.syncAll(appts, petNames)
                _attentionCount.value = appts.count { it.isInReminderWindow() }
            }
        }
    }

    fun logout() {
        syncJob?.cancel()
        syncJob = null
        _attentionCount.value = 0
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.LOGGED_OUT
        }
    }
}
