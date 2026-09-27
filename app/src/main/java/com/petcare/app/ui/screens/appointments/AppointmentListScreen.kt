package com.petcare.app.ui.screens.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.AppointmentFilter
import com.petcare.app.model.AppointmentItem
import com.petcare.app.ui.components.AppointmentCard
import com.petcare.app.ui.components.ConfirmDialog
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.speciesEmoji
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.AppointmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    onOpenAppointment: (String) -> Unit,
    onAddAppointment: () -> Unit,
    onAddPet: () -> Unit,
    viewModel: AppointmentViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<AppointmentItem?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    PetCareScaffold(
        title = "นัดหมาย / วัคซีน",
        isTabScreen = true,
        snackbarHostState = snackbar,
        floatingActionButton = {
            if (state.pets.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onAddAppointment,
                    containerColor = PetCareTheme.colors.amber,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) { Icon(Icons.Outlined.Add, contentDescription = "เพิ่มนัดหมาย") }
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
                message = "ต้องเพิ่มสัตว์เลี้ยงก่อนจึงจะบันทึกนัดหมายได้",
                actionLabel = "เพิ่มสัตว์เลี้ยง",
                onAction = onAddPet,
                modifier = Modifier.padding(padding),
            )
            return@PetCareScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ตัวกรองสถานะ
            item {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(AppointmentFilter.entries) { filter ->
                        val count = state.counts[filter] ?: 0
                        val isOverdue = filter == AppointmentFilter.OVERDUE && count > 0
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text("${filter.label} $count") },
                            colors = if (isOverdue) FilterChipDefaults.filterChipColors(
                                labelColor = PetCareTheme.colors.rust,
                                selectedContainerColor = PetCareTheme.colors.rustContainer,
                                selectedLabelColor = PetCareTheme.colors.onRustContainer,
                            ) else FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            // ตัวกรองสัตว์เลี้ยง
            if (state.pets.size > 1) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = state.petFilter == null,
                                onClick = { viewModel.setPetFilter(null) },
                                label = { Text("ทุกตัว") },
                            )
                        }
                        items(state.pets, key = { it.id }) { pet ->
                            FilterChip(
                                selected = state.petFilter == pet.id,
                                onClick = { viewModel.setPetFilter(if (state.petFilter == pet.id) null else pet.id) },
                                label = { Text("${speciesEmoji(pet.species)} ${pet.name}") },
                            )
                        }
                    }
                }
            }

            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventBusy,
                        title = "ไม่มีรายการ",
                        message = when (state.filter) {
                            AppointmentFilter.OVERDUE -> "เยี่ยม! ไม่มีนัดที่เลยกำหนด"
                            AppointmentFilter.DONE -> "ยังไม่มีนัดที่ทำเสร็จ"
                            else -> "กดปุ่ม + เพื่อเพิ่มนัดหมอหรือวัคซีน"
                        },
                    )
                }
            }

            items(state.items, key = { it.appointment.id }) { item ->
                SwipeToDelete(onDelete = { pendingDelete = item }) {
                    AppointmentCard(
                        item = item,
                        onClick = { onOpenAppointment(item.appointment.id) },
                        onToggleDone = { viewModel.toggleDone(item) },
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "ลบนัดหมายนี้?",
            message = "\"${item.appointment.title}\" จะถูกลบและยกเลิกการแจ้งเตือน",
            confirmLabel = "ลบ",
            onConfirm = { viewModel.delete(item) },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** ปัดซ้ายเพื่อลบ (ถามยืนยันก่อน การ์ดจึงเด้งกลับที่เดิม) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.padding(horizontal = 16.dp),
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("ลบ", color = PetCareTheme.colors.rust, style = MaterialTheme.typography.labelLarge)
            }
        },
    ) { content() }
}
