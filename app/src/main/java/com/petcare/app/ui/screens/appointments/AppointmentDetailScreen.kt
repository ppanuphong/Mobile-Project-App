package com.petcare.app.ui.screens.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.petcare.app.ui.theme.colors as statusColors
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.AppointmentDetailUiState
import com.petcare.app.viewmodel.AppointmentDetailViewModel
import com.petcare.app.viewmodel.TreatmentResultViewModel
import java.time.LocalDate

@Composable
fun AppointmentDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
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
        title = item?.appointment?.kindEnum?.label ?: "นัดหมาย",
        subtitle = item?.pet?.name,
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbar,
        actions = {
            if (item != null) {
                IconButton(onClick = { onEdit(item.appointment.id) }) { Icon(Icons.Outlined.Edit, contentDescription = "แก้ไขนัด") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "ลบนัด") }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            item == null -> EmptyState(
                icon = Icons.Outlined.EventBusy,
                title = "ไม่พบนัดหมาย",
                message = "นัดนี้อาจถูกลบไปแล้ว",
                modifier = Modifier.padding(padding),
            )
            else -> DetailContent(
                state = state,
                item = item,
                padding = padding,
                onRecordResult = { onRecordResult(item.appointment.id) },
                onToggleDone = viewModel::toggleDone,
                onOpenAppointment = onOpenAppointment,
                onAddProgress = { progressSheetOpen = true },
                onDeleteProgress = { pendingDeleteProgress = it },
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
    if (confirmDelete && item != null) {
        ConfirmDialog(
            title = "ลบนัดหมายนี้?",
            message = "\"${item.appointment.title}\" รวมถึงผลการรักษาและบันทึกอาการทั้งหมดจะถูกลบ",
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
private fun DetailContent(
    state: AppointmentDetailUiState,
    item: AppointmentItem,
    padding: PaddingValues,
    onRecordResult: () -> Unit,
    onToggleDone: () -> Unit,
    onOpenAppointment: (String) -> Unit,
    onAddProgress: () -> Unit,
    onDeleteProgress: (ProgressUpdate) -> Unit,
) {
    val appt = item.appointment
    val result = appt.result

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { InfoCard(item) }

        state.parent?.let { parent ->
            item {
                LinkCard(
                    label = "นัดนี้เป็นการติดตามผลจาก",
                    title = parent.appointment.title,
                    subtitle = DateUtils.toThaiDate(parent.appointment.date),
                    onClick = { onOpenAppointment(parent.appointment.id) },
                )
            }
        }

        // ยังไม่ได้บันทึกผล
        if (result == null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = PetCareTheme.colors.amberContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (appt.isDone) "ยังไม่ได้บันทึกผลการรักษา" else "พบหมอเสร็จแล้ว?",
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
                        if (!appt.isDone) {
                            TextButton(onClick = onToggleDone, modifier = Modifier.fillMaxWidth()) {
                                Text("ทำเครื่องหมายว่าเสร็จ โดยไม่บันทึกผล")
                            }
                        }
                    }
                }
            }
        } else {
            item { ResultCard(result, onEdit = onRecordResult) }
        }

        state.followUp?.let { followUp ->
            item { SectionHeader("นัดติดตามผล", modifier = Modifier.padding(top = 4.dp)) }
            item {
                AppointmentCard(
                    item = followUp,
                    onClick = { onOpenAppointment(followUp.appointment.id) },
                    onToggleDone = { onOpenAppointment(followUp.appointment.id) },
                )
            }
        }

        // ไทม์ไลน์ติดตามอาการ (มีหลังบันทึกผลแล้ว)
        if (result != null) {
            item {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ติดตามอาการ", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onAddProgress) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("อัปเดตอาการ")
                    }
                }
            }
            if (state.progress.isEmpty()) {
                item {
                    Text(
                        if (result.outcomeEnum == TreatmentOutcome.ONGOING)
                            "ยังไม่มีบันทึกอาการ กด \"อัปเดตอาการ\" เพื่อบันทึกว่าน้องเป็นอย่างไรบ้างหลังการรักษา"
                        else "ยังไม่มีบันทึกอาการ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.progress, key = { it.id }) { update ->
                TimelineRow(
                    update = update,
                    isLast = update == state.progress.last(),
                    onDelete = { onDeleteProgress(update) },
                )
            }
        }

        if (appt.isDone) {
            item {
                TextButton(onClick = onToggleDone, modifier = Modifier.fillMaxWidth()) {
                    Text("ย้ายกลับไปเป็นนัดที่รอดำเนินการ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(item: AppointmentItem) {
    val appt = item.appointment
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(appt.kindEnum.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(appt.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(item.status)
                if (item.status != DisplayStatus.DONE) {
                    Spacer(Modifier.width(8.dp))
                    Text(DateUtils.relativeLabel(appt.date), style = MaterialTheme.typography.labelMedium, color = item.status.statusColors().text)
                }
            }
            HorizontalDivider()
            item.pet?.let { pet ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PetAvatar(pet.species, pet.avatarColor, photo = pet.photo, size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(pet.name, style = MaterialTheme.typography.titleMedium)
                }
            }
            InfoLine("วันที่", buildString {
                append(DateUtils.toThaiDateWithDay(appt.date))
                if (appt.time.isNotBlank()) append(" เวลา ${appt.time} น.")
            })
            if (appt.clinic.isNotBlank()) InfoLine("คลินิก", appt.clinic)
            if (appt.vetName.isNotBlank()) InfoLine("สัตวแพทย์", appt.vetName)
            if (appt.notes.isNotBlank()) InfoLine("บันทึก", appt.notes)
            if (!appt.isDone) {
                InfoLine("แจ้งเตือน", if (appt.reminderDaysBefore == 0) "วันนัด" else "ล่วงหน้า ${appt.reminderDaysBefore} วัน")
            }
        }
    }
}

@Composable
private fun ResultCard(result: TreatmentResult, onEdit: () -> Unit) {
    Card(
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
