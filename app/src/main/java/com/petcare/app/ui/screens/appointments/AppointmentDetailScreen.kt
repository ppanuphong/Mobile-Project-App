package com.petcare.app.ui.screens.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.DisplayStatus
import com.petcare.app.ui.components.ConfirmDialog
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.StatusChip
import com.petcare.app.ui.components.icon
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.AppointmentDetailViewModel

/** หน้ารายละเอียดนัดหมาย (อ่านอย่างเดียว) กดดินสอเพื่อไปหน้าแก้ไขทั้งฟอร์ม */
@Composable
fun AppointmentDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenPet: (String) -> Unit,
    viewModel: AppointmentDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateUp()
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val item = state.item
    PetCareScaffold(
        title = "รายละเอียดนัดหมาย",
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbar,
        actions = {
            if (item != null) {
                IconButton(onClick = { onEdit(item.appointment.id) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "แก้ไข")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "ลบ")
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            item == null -> EmptyState(
                icon = Icons.Outlined.EventBusy,
                title = "ไม่พบนัดหมาย",
                message = "นัดหมายนี้อาจถูกลบไปแล้ว",
                modifier = Modifier.padding(padding),
            )
            else -> AppointmentDetailContent(
                item = item,
                onOpenPet = onOpenPet,
                onToggleDone = viewModel::toggleDone,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "ลบนัดหมายนี้?",
            message = "นัดหมายและการแจ้งเตือนของนัดนี้จะถูกลบถาวร",
            confirmLabel = "ลบ",
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun AppointmentDetailContent(
    item: AppointmentItem,
    onOpenPet: (String) -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appt = item.appointment
    val pet = item.pet
    val isDone = item.status == DisplayStatus.DONE
    val outlined = CardDefaults.outlinedCardBorder()
    val surface = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // หัวข้อนัด
        Card(Modifier.fillMaxWidth(), colors = surface, border = outlined) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        appt.kindEnum.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(appt.kindEnum.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text(appt.title, style = MaterialTheme.typography.headlineSmall)
                if (item.status == DisplayStatus.OVERDUE || isDone) StatusChip(item.status)
            }
        }

        // สัตว์เลี้ยง
        if (pet != null) {
            Card(onClick = { onOpenPet(pet.id) }, modifier = Modifier.fillMaxWidth(), colors = surface, border = outlined) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val species = appt.species.ifBlank { pet.species }
                    PetAvatar(species, pet.avatarColor, size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val kind = listOf(species, appt.breed.ifBlank { pet.breed })
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                        if (kind.isNotBlank()) {
                            Text(
                                kind,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // รายละเอียด
        Card(Modifier.fillMaxWidth(), colors = surface, border = outlined) {
            Column(Modifier.padding(vertical = 6.dp)) {
                DetailRow(Icons.Outlined.CalendarMonth, "วันที่", DateUtils.toThaiDateWithDay(appt.date))
                DetailRow(Icons.Outlined.Schedule, "เวลา", if (appt.time.isBlank()) "ไม่ระบุ" else "${appt.time} น.")
                if (appt.vetName.isNotBlank()) DetailRow(Icons.Outlined.MedicalServices, "สัตวแพทย์", appt.vetName)
                if (appt.clinic.isNotBlank()) DetailRow(Icons.Outlined.LocalHospital, "คลินิก / โรงพยาบาลสัตว์", appt.clinic)
                DetailRow(
                    Icons.Outlined.NotificationsActive,
                    "แจ้งเตือนล่วงหน้า",
                    if (appt.reminderDaysBefore == 0) "วันนัด" else "${appt.reminderDaysBefore} วัน",
                )
                if (appt.notes.isNotBlank()) DetailRow(Icons.AutoMirrored.Outlined.Notes, "บันทึกเพิ่มเติม", appt.notes)
            }
        }

        Spacer(Modifier.height(4.dp))
        if (isDone) {
            OutlinedButton(onClick = onToggleDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("ยกเลิกสถานะเสร็จ")
            }
        } else {
            Button(onClick = onToggleDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("ทำเครื่องหมายว่าเสร็จ")
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
