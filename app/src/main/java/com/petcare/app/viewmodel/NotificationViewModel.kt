package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.PetRepository
import com.petcare.app.data.SessionManager
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.DisplayStatus
import com.petcare.app.model.withPets
import com.petcare.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduledReminder(val item: AppointmentItem, val remindOn: LocalDate)

data class NotificationUiState(
    val isLoading: Boolean = true,
    val enabled: Boolean = true,
    /** ถึงช่วงเตือนแล้ว (ใกล้ถึงวันนัด) */
    val active: List<AppointmentItem> = emptyList(),
    /** เลยกำหนดแล้วแต่ยังไม่ทำเครื่องหมายว่าเสร็จ */
    val overdue: List<AppointmentItem> = emptyList(),
    /** การเตือนที่ตั้งไว้ล่วงหน้า */
    val scheduled: List<ScheduledReminder> = emptyList(),
)

/** แท็บแจ้งเตือน: รายการที่ต้องใส่ใจ + ตั้งค่าเปิด/ปิดการแจ้งเตือน */
class NotificationViewModel(
    petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = combine(
        petRepository.observePets(),
        appointmentRepository.observeAppointments(),
        sessionManager.notificationsEnabled,
    ) { pets, appointments, enabled ->
        val today = LocalDate.now()
        val pending = appointments.withPets(pets, today).filter { it.status != DisplayStatus.DONE }
        NotificationUiState(
            isLoading = false,
            enabled = enabled,
            overdue = pending.filter { it.status == DisplayStatus.OVERDUE }.sortedByDescending { it.appointment.date },
            active = pending.filter { it.status != DisplayStatus.OVERDUE && it.appointment.isInReminderWindow(today) },
            scheduled = pending
                .filter { !it.appointment.isInReminderWindow(today) }
                .mapNotNull { item ->
                    ReminderScheduler.triggerTime(item.appointment)?.let { ScheduledReminder(item, it.toLocalDate()) }
                },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationUiState())

    fun markDone(item: AppointmentItem) {
        viewModelScope.launch {
            runCatching { appointmentRepository.setDone(item.appointment, true, item.pet?.name.orEmpty()) }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { sessionManager.setNotificationsEnabled(enabled) }
    }
}
