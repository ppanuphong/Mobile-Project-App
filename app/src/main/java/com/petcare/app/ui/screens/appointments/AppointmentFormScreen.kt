package com.petcare.app.ui.screens.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.AppointmentKind
import com.petcare.app.notification.ReminderScheduler
import com.petcare.app.ui.components.DatePickerField
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.TimePickerField
import com.petcare.app.ui.components.icon
import com.petcare.app.ui.components.speciesEmoji
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.AppointmentFormViewModel
import com.petcare.app.viewmodel.AppointmentFormViewModel.Companion.FIELD_DATE
import com.petcare.app.viewmodel.AppointmentFormViewModel.Companion.FIELD_PET
import com.petcare.app.viewmodel.AppointmentFormViewModel.Companion.FIELD_TIME
import com.petcare.app.viewmodel.AppointmentFormViewModel.Companion.FIELD_TITLE

private val REMINDER_OPTIONS = listOf(0, 1, 2, 3, 7, 14)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentFormScreen(
    onDone: () -> Unit,
    onAddPet: () -> Unit,
    viewModel: AppointmentFormViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    PetCareScaffold(
        title = if (state.isEdit) "แก้ไขนัดหมาย" else "เพิ่มนัดหมาย",
        onNavigateUp = onDone,
        snackbarHostState = snackbar,
        actions = {
            TextButton(onClick = viewModel::save, enabled = !state.isSaving && !state.isLoading) {
                Text("บันทึก", color = PetCareTheme.colors.onTopBar)
            }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@PetCareScaffold
        }
        if (state.pets.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Pets,
                title = "ยังไม่มีสัตว์เลี้ยง",
                message = "เพิ่มสัตว์เลี้ยงก่อน แล้วค่อยกลับมาบันทึกนัดหมาย",
                actionLabel = "เพิ่มสัตว์เลี้ยง",
                onAction = onAddPet,
                modifier = Modifier.padding(padding),
            )
            return@PetCareScaffold
        }

        val errors = state.errors
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("สัตว์เลี้ยง *", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.pets.forEach { pet ->
                    FilterChip(
                        selected = state.petId == pet.id,
                        onClick = { viewModel.onPetChange(pet.id) },
                        label = { Text("${speciesEmoji(pet.species)} ${pet.name}") },
                    )
                }
            }
            errors[FIELD_PET]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Text("ประเภท", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppointmentKind.entries.forEach { kind ->
                    FilterChip(
                        selected = state.kind == kind,
                        onClick = { viewModel.onKindChange(kind) },
                        label = { Text(kind.label) },
                        leadingIcon = { Icon(kind.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("หัวข้อ *") },
                placeholder = { Text(viewModel.suggestedTitle(state.kind)) },
                isError = errors[FIELD_TITLE] != null,
                supportingText = errors[FIELD_TITLE]?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    value = state.date,
                    onValueChange = viewModel::onDateChange,
                    label = "วันที่ *",
                    error = errors[FIELD_DATE],
                    modifier = Modifier.weight(1.4f),
                )
                TimePickerField(
                    value = state.time,
                    onValueChange = viewModel::onTimeChange,
                    label = "เวลา",
                    error = errors[FIELD_TIME],
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = state.vetName,
                onValueChange = viewModel::onVetNameChange,
                label = { Text("ชื่อสัตวแพทย์") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.clinic,
                onValueChange = viewModel::onClinicChange,
                label = { Text("คลินิก / โรงพยาบาลสัตว์") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("บันทึกเพิ่มเติม") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            // การแจ้งเตือนล่วงหน้า
            Card(
                colors = CardDefaults.cardColors(containerColor = PetCareTheme.colors.amberContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = PetCareTheme.colors.onAmberContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "แจ้งเตือนล่วงหน้า",
                            style = MaterialTheme.typography.titleSmall,
                            color = PetCareTheme.colors.onAmberContainer,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        REMINDER_OPTIONS.forEach { days ->
                            FilterChip(
                                selected = state.reminderDaysBefore == days,
                                onClick = { viewModel.onReminderChange(days) },
                                label = { Text(if (days == 0) "วันนัด" else "$days วัน") },
                                // พื้นการ์ดเป็นสีอำพันอ่อนอยู่แล้ว ชิปที่เลือกจึงใช้อำพันเข้มให้เห็นชัด
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = PetCareTheme.colors.onAmberContainer,
                                    selectedContainerColor = PetCareTheme.colors.amber,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                            )
                        }
                    }
                    val remindDate = DateUtils.parseDate(state.date)?.minusDays(state.reminderDaysBefore.toLong())
                    if (remindDate != null) {
                        Text(
                            "จะแจ้งเตือนวันที่ ${DateUtils.toThaiDateWithDay(DateUtils.formatDate(remindDate))} " +
                                "เวลา ${DateUtils.formatTime(ReminderScheduler.REMINDER_TIME)} น.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PetCareTheme.colors.onAmberContainer,
                        )
                    }
                }
            }

            if (state.isEdit) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ทำเสร็จแล้ว", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "นัดที่เสร็จแล้วจะย้ายไปอยู่ในประวัติ และไม่แจ้งเตือนอีก",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.isDone, onCheckedChange = viewModel::onDoneChange)
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.isEdit) "บันทึกการแก้ไข" else "บันทึกนัดหมาย")
                }
            }
        }
    }
}
