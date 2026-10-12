package com.petcare.app.ui.screens.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.petcare.app.model.PetCondition
import com.petcare.app.model.ProgressUpdate
import com.petcare.app.model.TreatmentOutcome
import com.petcare.app.model.TreatmentResult
import com.petcare.app.ui.components.AppointmentCard
import com.petcare.app.ui.components.ConditionChip
import com.petcare.app.ui.components.ConfirmDialog
import com.petcare.app.ui.components.DatePickerField
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.OutcomeChip
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.SectionHeader
import com.petcare.app.ui.components.StatusChip
import com.petcare.app.ui.components.colors
import com.petcare.app.ui.components.icon
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.AppointmentDetailUiState
import com.petcare.app.viewmodel.AppointmentDetailViewModel
import com.petcare.app.viewmodel.TreatmentResultViewModel
import java.time.LocalDate

/**
 * หน้ารายละเอียดนัดหมาย: ข้อมูลนัด → ผลการรักษา → นัดติดตามผล → ไทม์ไลน์ติดตามอาการ
 * กดดินสอเพื่อไปหน้าแก้ไขทั้งฟอร์ม
 */
@Composable
fun AppointmentDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenPet: (String) -> Unit,
    onRecordResult: (String) -> Unit,
    onOpenAppointment: (String) -> Unit,
    viewModel: AppointmentDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var progressSheetOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteProgress by remember { mutableStateOf<ProgressUpdate?>(null) }

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
                state = state,
                item = item,
                onOpenPet = onOpenPet,
                onToggleDone = viewModel::toggleDone,
                onRecordResult = { onRecordResult(item.appointment.id) },
                onOpenAppointment = onOpenAppointment,
                onAddProgress = { progressSheetOpen = true },
                onDeleteProgress = { pendingDeleteProgress = it },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (progressSheetOpen) {
        ProgressSheet(
            onDismiss = { progressSheetOpen = false },
            onSave = { date, condition, note ->
                viewModel.addProgress(date, condition, note)
                progressSheetOpen = false
            },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "ลบนัดหมายนี้?",
            message = "นัดหมาย การแจ้งเตือน ผลการรักษา และบันทึกอาการของนัดนี้จะถูกลบถาวร",
            confirmLabel = "ลบ",
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
    pendingDeleteProgress?.let { update ->
        ConfirmDialog(
            title = "ลบบันทึกอาการ?",
            message = "บันทึกวันที่ ${DateUtils.toThaiDate(update.date)} (${update.conditionEnum.label}) จะถูกลบ",
            confirmLabel = "ลบ",
            onConfirm = { viewModel.deleteProgress(update) },
            onDismiss = { pendingDeleteProgress = null },
        )
    }
}

@Composable
private fun AppointmentDetailContent(
    state: AppointmentDetailUiState,
    item: AppointmentItem,
    onOpenPet: (String) -> Unit,
    onToggleDone: () -> Unit,
    onRecordResult: () -> Unit,
    onOpenAppointment: (String) -> Unit,
    onAddProgress: () -> Unit,
    onDeleteProgress: (ProgressUpdate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appt = item.appointment
    val pet = item.pet
    val result = appt.result
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
                    PetAvatar(species, pet.avatarColor, photo = pet.photo, size = 48.dp)
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

        // นัดนี้เป็นนัดติดตามผลของนัดก่อนหน้า
        state.parent?.let { parent ->
            LinkCard(
                label = "นัดนี้เป็นการติดตามผลจาก",
                title = parent.appointment.title,
                subtitle = DateUtils.toThaiDate(parent.appointment.date),
                onClick = { onOpenAppointment(parent.appointment.id) },
            )
        }

        // ผลการรักษา
        if (result == null) {
            RecordResultPrompt(isDone = isDone, onRecordResult = onRecordResult)
        } else {
            ResultCard(result, onEdit = onRecordResult)
        }

        // นัดติดตามผลที่สร้างจากผลการรักษานี้
        state.followUp?.let { followUp ->
            SectionHeader("นัดติดตามผล", modifier = Modifier.padding(top = 4.dp))
            AppointmentCard(
                item = followUp,
                onClick = { onOpenAppointment(followUp.appointment.id) },
                onToggleDone = { onOpenAppointment(followUp.appointment.id) },
            )
        }

        // ไทม์ไลน์ติดตามอาการ (มีหลังบันทึกผลแล้ว)
        if (result != null) {
            ProgressSection(
                result = result,
                progress = state.progress,
                onAddProgress = onAddProgress,
                onDeleteProgress = onDeleteProgress,
            )
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
                Text(if (result == null) "ทำเครื่องหมายว่าเสร็จ (ไม่บันทึกผล)" else "ทำเครื่องหมายว่าเสร็จ")
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

/** ชวนบันทึกผลการรักษา (ยังไม่มีผล) */
@Composable
private fun RecordResultPrompt(isDone: Boolean, onRecordResult: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PetCareTheme.colors.amberContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (isDone) "ยังไม่ได้บันทึกผลการรักษา" else "พบหมอเสร็จแล้ว?",
                style = MaterialTheme.typography.titleMedium,
                color = PetCareTheme.colors.onAmberContainer,
            )
            Text(
                "บันทึกผลวินิจฉัย การรักษา ยาที่ได้รับ และนัดติดตามผล เพื่อดูย้อนหลังและติดตามอาการต่อได้",
                style = MaterialTheme.typography.bodySmall,
                color = PetCareTheme.colors.onAmberContainer,
            )
            Button(
                onClick = onRecordResult,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PetCareTheme.colors.amber,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.MedicalInformation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("บันทึกผลการรักษา")
            }
        }
    }
}

@Composable
private fun ResultCard(result: TreatmentResult, onEdit: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("ผลการรักษา", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "แก้ไขผลการรักษา") }
            }
            Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutcomeChip(result.outcomeEnum)
                if (result.diagnosis.isNotBlank()) InfoLine("ผลวินิจฉัย / อาการที่พบ", result.diagnosis)
                if (result.treatment.isNotBlank()) InfoLine("การรักษาที่ได้รับ", result.treatment)
                if (result.medications.isNotBlank()) InfoLine("ยาที่ได้รับ", result.medications)
                if (result.weightKg > 0 || result.cost > 0) {
                    Row {
                        if (result.weightKg > 0) {
                            InfoLine("น้ำหนัก", "${TreatmentResultViewModel.formatNumber(result.weightKg)} กก.", Modifier.weight(1f))
                        }
                        if (result.cost > 0) {
                            InfoLine("ค่าใช้จ่าย", "${"%,.0f".format(result.cost)} บาท", Modifier.weight(1f))
                        }
                    }
                }
                if (result.recordedAt.isNotBlank()) {
                    Text(
                        "บันทึกเมื่อ ${DateUtils.toThaiDate(result.recordedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinkCard(label: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ProgressSection(
    result: TreatmentResult,
    progress: List<ProgressUpdate>,
    onAddProgress: () -> Unit,
    onDeleteProgress: (ProgressUpdate) -> Unit,
) {
    Column {
        Row(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ติดตามอาการ", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onAddProgress) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("อัปเดตอาการ")
            }
        }
        if (progress.isEmpty()) {
            Text(
                if (result.outcomeEnum == TreatmentOutcome.ONGOING)
                    "ยังไม่มีบันทึกอาการ กด \"อัปเดตอาการ\" เพื่อบันทึกว่าน้องเป็นอย่างไรบ้างหลังการรักษา"
                else "ยังไม่มีบันทึกอาการ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        progress.forEachIndexed { index, update ->
            TimelineRow(update = update, isLast = index == progress.lastIndex, onDelete = { onDeleteProgress(update) })
        }
    }
}

/** แถวในไทม์ไลน์: จุดสีตามอาการ + เส้นเชื่อม + รายละเอียด */
@Composable
private fun TimelineRow(update: ProgressUpdate, isLast: Boolean, onDelete: () -> Unit) {
    val colors = update.conditionEnum.colors()
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(Modifier.width(20.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 6.dp).size(12.dp).background(colors.accent, CircleShape))
            if (!isLast) {
                Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DateUtils.toThaiDateWithDay(update.date),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                ConditionChip(update.conditionEnum)
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "ลบบันทึกนี้", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (update.note.isNotBlank()) {
                Text(update.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProgressSheet(onDismiss: () -> Unit, onSave: (String, PetCondition, String) -> Unit) {
    var date by rememberSaveable { mutableStateOf(DateUtils.formatDate(LocalDate.now())) }
    var condition by rememberSaveable { mutableStateOf(PetCondition.BETTER) }
    var note by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("อัปเดตอาการ", style = MaterialTheme.typography.titleLarge)
            DatePickerField(
                value = date,
                onValueChange = { date = it },
                label = "วันที่สังเกตอาการ",
                allowFuture = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("อาการตอนนี้", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PetCondition.entries.forEach { option ->
                    FilterChip(
                        selected = condition == option,
                        onClick = { condition = option },
                        label = { Text(option.label) },
                    )
                }
            }
            if (condition == PetCondition.RECOVERED) {
                Text(
                    "เลือก \"หายดี\" จะปิดเคสนี้เป็น \"หายดีแล้ว\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("รายละเอียด (เช่น กินอาหารได้มากขึ้น แผลแห้งดี)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onSave(date, condition, note) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("บันทึกอาการ")
            }
        }
    }
}
