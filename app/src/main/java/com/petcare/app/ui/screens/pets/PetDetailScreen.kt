package com.petcare.app.ui.screens.pets

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.Pet
import com.petcare.app.ui.components.AppointmentCard
import com.petcare.app.ui.components.ConfirmDialog
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.theme.NumberStyle
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.PetDetailUiState
import com.petcare.app.viewmodel.PetDetailViewModel
import com.petcare.app.viewmodel.PetHistoryTab

@Composable
fun PetDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    onAddAppointment: (String) -> Unit,
    onOpenAppointment: (String) -> Unit,
    viewModel: PetDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
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

    val pet = state.pet
    PetCareScaffold(
        title = pet?.name ?: "สัตว์เลี้ยง",
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbar,
        actions = {
            if (pet != null) {
                IconButton(onClick = { onEdit(pet.id) }) { Icon(Icons.Outlined.Edit, contentDescription = "แก้ไข") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "ลบ") }
            }
        },
        floatingActionButton = {
            if (pet != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddAppointment(pet.id) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("เพิ่มนัด") },
                    containerColor = PetCareTheme.colors.amber,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            pet == null -> EmptyState(
                icon = Icons.Outlined.History,
                title = "ไม่พบข้อมูล",
                message = "สัตว์เลี้ยงตัวนี้อาจถูกลบไปแล้ว",
                modifier = Modifier.padding(padding),
            )
            else -> PetDetailContent(
                pet = pet,
                state = state,
                padding = padding,
                onSelectTab = viewModel::selectTab,
                onOpenAppointment = onOpenAppointment,
                onToggleDone = viewModel::toggleDone,
            )
        }
    }

    if (confirmDelete && pet != null) {
        ConfirmDialog(
            title = "ลบ ${pet.name}?",
            message = "ข้อมูลและประวัตินัดหมาย/วัคซีนทั้งหมดของ ${pet.name} จะถูกลบถาวร",
            confirmLabel = "ลบ",
            onConfirm = viewModel::deletePet,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PetDetailContent(
    pet: Pet,
    state: PetDetailUiState,
    padding: PaddingValues,
    onSelectTab: (PetHistoryTab) -> Unit,
    onOpenAppointment: (String) -> Unit,
    onToggleDone: (AppointmentItem) -> Unit,
) {
    val list = when (state.tab) {
        PetHistoryTab.UPCOMING -> state.upcoming
        PetHistoryTab.VACCINES -> state.vaccineHistory
        PetHistoryTab.VISITS -> state.visitHistory
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ProfileHeader(pet, state) }

        item {
            PrimaryTabRow(selectedTabIndex = state.tab.ordinal, containerColor = MaterialTheme.colorScheme.background) {
                PetHistoryTab.entries.forEach { tab ->
                    val count = when (tab) {
                        PetHistoryTab.UPCOMING -> state.upcoming.size
                        PetHistoryTab.VACCINES -> state.vaccineHistory.size
                        PetHistoryTab.VISITS -> state.visitHistory.size
                    }
                    Tab(
                        selected = state.tab == tab,
                        onClick = { onSelectTab(tab) },
                        text = { Text("${tab.label} ($count)", style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }
        }

        if (list.isEmpty()) {
            item {
                EmptyState(
                    icon = if (state.tab == PetHistoryTab.VACCINES) Icons.Outlined.Vaccines else Icons.Outlined.EventAvailable,
                    title = when (state.tab) {
                        PetHistoryTab.UPCOMING -> "ไม่มีนัดที่รออยู่"
                        PetHistoryTab.VACCINES -> "ยังไม่มีประวัติวัคซีน"
                        PetHistoryTab.VISITS -> "ยังไม่มีประวัติหาหมอ"
                    },
                    message = when (state.tab) {
                        PetHistoryTab.UPCOMING -> "กด \"เพิ่มนัด\" เพื่อบันทึกนัดหมอหรือวัคซีนครั้งถัดไป"
                        else -> "นัดที่ทำเครื่องหมายว่าเสร็จแล้วจะแสดงที่นี่"
                    },
                )
            }
        }

        items(list, key = { it.appointment.id }) { item ->
            AppointmentCard(
                item = item,
                showPet = false,
                onClick = { onOpenAppointment(item.appointment.id) },
                onToggleDone = { onToggleDone(item) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun ProfileHeader(pet: Pet, state: PetDetailUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PetAvatar(pet.species, pet.avatarColor, photo = pet.photo, size = 72.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(pet.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOf(pet.species, pet.breed).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCell(
                    "วันเกิด",
                    if (pet.birthday.isBlank()) "ไม่ทราบ" else DateUtils.toThaiDate(pet.birthday),
                    Modifier.weight(1.3f),
                )
                InfoCell("อายุ", DateUtils.ageLabel(pet.birthday), Modifier.weight(1f))
            }
            Spacer(Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountCell("วัคซีนที่ฉีดแล้ว", state.vaccineHistory.size, Modifier.weight(1f))
                CountCell("หาหมอ/ดูแลแล้ว", state.visitHistory.size, Modifier.weight(1f))
                CountCell("นัดที่รออยู่", state.upcoming.size, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun CountCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(value.toString(), style = NumberStyle, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
