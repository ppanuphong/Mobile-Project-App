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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.TreatmentOutcome
import com.petcare.app.ui.components.DatePickerField
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.TimePickerField
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.TreatmentResultViewModel
import com.petcare.app.viewmodel.TreatmentResultViewModel.Companion.FIELD_COST
import com.petcare.app.viewmodel.TreatmentResultViewModel.Companion.FIELD_DIAGNOSIS
import com.petcare.app.viewmodel.TreatmentResultViewModel.Companion.FIELD_FOLLOW_UP_DATE
import com.petcare.app.viewmodel.TreatmentResultViewModel.Companion.FIELD_WEIGHT

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TreatmentResultScreen(
    onDone: () -> Unit,
    viewModel: TreatmentResultViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
        title = if (state.isEdit) "แก้ไขผลการรักษา" else "บันทึกผลการรักษา",
        subtitle = state.appointment?.title,
        onNavigateUp = onDone,
        snackbarHostState = snackbar,
        actions = {
            TextButton(onClick = viewModel::save, enabled = !state.isSaving && state.appointment != null) {
                Text("บันทึก", color = PetCareTheme.colors.onTopBar)
            }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@PetCareScaffold
        }
        val appt = state.appointment ?: return@PetCareScaffold
        val errors = state.errors

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // นัดที่กำลังบันทึกผล
            Row(verticalAlignment = Alignment.CenterVertically) {
                state.pet?.let { PetAvatar(it.species, it.avatarColor, photo = it.photo, size = 44.dp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(appt.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            state.pet?.name,
                            DateUtils.toThaiDate(appt.date),
                            appt.clinic.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = state.diagnosis,
                onValueChange = viewModel::onDiagnosisChange,
                label = { Text("ผลวินิจฉัย / อาการที่พบ") },
                placeholder = { Text("เช่น ผิวหนังอักเสบจากเชื้อรา, สุขภาพแข็งแรงดี") },
                isError = errors[FIELD_DIAGNOSIS] != null,
                supportingText = errors[FIELD_DIAGNOSIS]?.let { { Text(it) } },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.treatment,
                onValueChange = viewModel::onTreatmentChange,
                label = { Text("การรักษาที่ได้รับ") },
                placeholder = { Text("เช่น ฉีดยาฆ่าเชื้อ ทำแผล ฉีดวัคซีนเข็มที่ 2") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.medications,
                onValueChange = viewModel::onMedicationsChange,
                label = { Text("ยาที่ได้รับและวิธีใช้") },
                placeholder = { Text("เช่น ยาฆ่าเชื้อ 1 เม็ด เช้า-เย็น 7 วัน") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.weightKg,
                    onValueChange = viewModel::onWeightChange,
                    label = { Text("น้ำหนัก (กก.)") },
                    isError = errors[FIELD_WEIGHT] != null,
                    supportingText = errors[FIELD_WEIGHT]?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.cost,
                    onValueChange = viewModel::onCostChange,
                    label = { Text("ค่าใช้จ่าย (บาท)") },
                    isError = errors[FIELD_COST] != null,
                    supportingText = errors[FIELD_COST]?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            Text("ผลลัพธ์", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TreatmentOutcome.entries.forEach { outcome ->
                    FilterChip(
                        selected = state.outcome == outcome,
                        onClick = { viewModel.onOutcomeChange(outcome) },
                        label = { Text(outcome.label) },
                    )
                }
            }
            Text(
                when (state.outcome) {
                    TreatmentOutcome.NORMAL -> "ไม่มีอาการที่ต้องติดตาม เช่น ฉีดวัคซีน ตรวจสุขภาพปกติ"
                    TreatmentOutcome.ONGOING -> "ยังต้องติดตามอาการ จะแสดงในหน้าหลักจนกว่าจะบันทึกว่าหายดี"
                    TreatmentOutcome.RECOVERED -> "รักษาจนหายดีแล้ว"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // นัดติดตามผล
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("นัดติดตามผล", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (state.hasExistingFollowUp) "สร้างนัดติดตามผลไว้แล้ว ดูได้ในหน้ารายละเอียดนัด"
                                else "สร้างนัดใหม่ให้อัตโนมัติ พร้อมแจ้งเตือนล่วงหน้า 1 วัน",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!state.hasExistingFollowUp) {
                            Switch(checked = state.createFollowUp, onCheckedChange = viewModel::onCreateFollowUpChange)
                        }
                    }
                    if (state.createFollowUp && !state.hasExistingFollowUp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DatePickerField(
                                value = state.followUpDate,
                                onValueChange = viewModel::onFollowUpDateChange,
                                label = "วันนัด",
                                error = errors[FIELD_FOLLOW_UP_DATE],
                                modifier = Modifier.weight(1.4f),
                            )
                            TimePickerField(
                                value = state.followUpTime,
                                onValueChange = viewModel::onFollowUpTimeChange,
                                label = "เวลา",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
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
                    Text(if (state.isEdit) "บันทึกการแก้ไข" else "บันทึกผล และปิดนัดนี้")
                }
            }
        }
    }
}
