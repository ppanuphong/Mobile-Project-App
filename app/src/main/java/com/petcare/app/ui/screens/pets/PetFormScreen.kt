package com.petcare.app.ui.screens.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.AVATAR_COLORS
import com.petcare.app.model.SPECIES_OPTIONS
import com.petcare.app.ui.components.DatePickerField
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.speciesEmoji
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.PetFormViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetFormScreen(
    onDone: () -> Unit,
    viewModel: PetFormViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.savedId) {
        if (state.savedId != null) onDone()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    PetCareScaffold(
        title = if (state.isEdit) "แก้ไขข้อมูลสัตว์เลี้ยง" else "เพิ่มสัตว์เลี้ยง",
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

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PetAvatar(state.species, state.avatarColor, size = 96.dp)
            }

            Text("สีประจำตัว", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AVATAR_COLORS.forEach { color ->
                    val selected = color == state.avatarColor
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { viewModel.onColorChange(color) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = "เลือกแล้ว", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("ชื่อสัตว์เลี้ยง *") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("ชนิด *", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SPECIES_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = state.species == option,
                        onClick = { viewModel.onSpeciesChange(option) },
                        label = { Text("${speciesEmoji(option)} $option") },
                    )
                }
            }
            OutlinedTextField(
                value = state.species,
                onValueChange = viewModel::onSpeciesChange,
                label = { Text("หรือพิมพ์ชนิดเอง") },
                isError = state.speciesError != null,
                supportingText = state.speciesError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.breed,
                onValueChange = viewModel::onBreedChange,
                label = { Text("สายพันธุ์") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DatePickerField(
                value = state.birthday,
                onValueChange = viewModel::onBirthdayChange,
                label = "วันเกิด",
                placeholder = "ไม่ทราบ",
                error = state.birthdayError,
                allowFuture = false,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.birthday.isNotBlank()) {
                Text(
                    "อายุ ${DateUtils.ageLabel(state.birthday)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { viewModel.onBirthdayChange("") }) { Text("ล้างวันเกิด") }
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
                    Text(if (state.isEdit) "บันทึกการแก้ไข" else "เพิ่มสัตว์เลี้ยง")
                }
            }
        }
    }
}
