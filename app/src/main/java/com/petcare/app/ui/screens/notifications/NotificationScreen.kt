package com.petcare.app.ui.screens.notifications

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.AppointmentItem
import com.petcare.app.notification.NotificationHelper
import com.petcare.app.ui.components.AppointmentCard
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.SectionHeader
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.NotificationViewModel
import com.petcare.app.viewmodel.ScheduledReminder

@Composable
fun NotificationScreen(
    onOpenAppointment: (String) -> Unit,
    viewModel: NotificationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ตรวจสิทธิ์ใหม่ทุกครั้งที่กลับมาหน้านี้ (ผู้ใช้อาจไปเปิดในการตั้งค่าระบบ)
    var systemAllowed by remember { mutableStateOf(NotificationHelper.canPostNotifications(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        systemAllowed = NotificationHelper.canPostNotifications(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        systemAllowed = NotificationHelper.canPostNotifications(context)
    }
    val requestSystemPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    PetCareScaffold(title = "แจ้งเตือน", isTabScreen = true) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@PetCareScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingsCard(
                    enabled = state.enabled,
                    systemAllowed = systemAllowed,
                    onToggle = viewModel::setEnabled,
                    onRequestPermission = requestSystemPermission,
                    onTest = {
                        NotificationHelper.show(
                            context,
                            TEST_NOTIFICATION_ID,
                            "ทดสอบการแจ้งเตือน",
                            "ถ้าเห็นข้อความนี้ แปลว่าแอปแจ้งเตือนนัดหมอได้แล้ว 🐾",
                        )
                    },
                )
            }

            if (state.overdue.isEmpty() && state.active.isEmpty() && state.scheduled.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "ไม่มีการแจ้งเตือน",
                        message = "เมื่อเพิ่มนัดหมาย แอปจะเตือนล่วงหน้าตามจำนวนวันที่ตั้งไว้",
                    )
                }
            }

            if (state.overdue.isNotEmpty()) {
                item { SectionHeader("เลยกำหนด ยังไม่ได้ทำ", count = state.overdue.size, modifier = Modifier.padding(top = 8.dp)) }
                items(state.overdue, key = { "o_" + it.appointment.id }) { item ->
                    ReminderCard(item, onOpenAppointment, viewModel::markDone)
                }
            }
            if (state.active.isNotEmpty()) {
                item { SectionHeader("ถึงเวลาเตือนแล้ว", count = state.active.size, modifier = Modifier.padding(top = 8.dp)) }
                items(state.active, key = { "a_" + it.appointment.id }) { item ->
                    ReminderCard(item, onOpenAppointment, viewModel::markDone)
                }
            }
            if (state.scheduled.isNotEmpty()) {
                item { SectionHeader("ตั้งเตือนไว้ล่วงหน้า", count = state.scheduled.size, modifier = Modifier.padding(top = 8.dp)) }
                items(state.scheduled, key = { "s_" + it.item.appointment.id }) { reminder ->
                    ScheduledRow(reminder, onClick = { onOpenAppointment(reminder.item.appointment.id) })
                }
            }
        }
    }
}

private const val TEST_NOTIFICATION_ID = 9_999

@Composable
private fun ReminderCard(item: AppointmentItem, onOpenAppointment: (String) -> Unit, onMarkDone: (AppointmentItem) -> Unit) {
    AppointmentCard(
        item = item,
        onClick = { onOpenAppointment(item.appointment.id) },
        onToggleDone = { onMarkDone(item) },
    )
}

@Composable
private fun SettingsCard(
    enabled: Boolean,
    systemAllowed: Boolean,
    onToggle: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onTest: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = if (enabled) PetCareTheme.colors.amber else MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("แจ้งเตือนนัดหมอ / วัคซีน", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "เตือนเวลา 08:00 น. ล่วงหน้าตามที่ตั้งในแต่ละนัด",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (enabled && !systemAllowed) {
                Card(colors = CardDefaults.cardColors(containerColor = PetCareTheme.colors.rustContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "ระบบ Android ยังไม่อนุญาตให้แอปแสดงการแจ้งเตือน",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PetCareTheme.colors.onRustContainer,
                        )
                        OutlinedButton(onClick = onRequestPermission, modifier = Modifier.padding(top = 8.dp)) {
                            Text("อนุญาตการแจ้งเตือน")
                        }
                    }
                }
            }
            if (enabled && systemAllowed) {
                OutlinedButton(onClick = onTest) { Text("ส่งการแจ้งเตือนทดสอบ") }
            }
        }
    }
}

@Composable
private fun ScheduledRow(reminder: ScheduledReminder, onClick: () -> Unit) {
    val appt = reminder.item.appointment
    val pet = reminder.item.pet
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            leadingContent = {
                if (pet != null) PetAvatar(pet.species, pet.avatarColor, size = 40.dp)
                else Icon(Icons.Outlined.Schedule, contentDescription = null)
            },
            headlineContent = { Text(appt.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    "เตือน ${DateUtils.toThaiDate(DateUtils.formatDate(reminder.remindOn))} · " +
                        "นัด ${DateUtils.toThaiDate(appt.date)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            trailingContent = {
                Text(
                    DateUtils.relativeLabel(DateUtils.formatDate(reminder.remindOn)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
}
