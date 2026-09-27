package com.petcare.app.ui.screens.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.model.Pet
import com.petcare.app.ui.components.ConfirmDialog
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.PetListItem
import com.petcare.app.viewmodel.PetViewModel

@Composable
fun PetListScreen(
    onOpenPet: (String) -> Unit,
    onAddPet: () -> Unit,
    onEditPet: (String) -> Unit,
    viewModel: PetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Pet?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    PetCareScaffold(
        title = "สัตว์เลี้ยง",
        subtitle = if (state.pets.isNotEmpty() || state.query.isNotBlank()) "ทั้งหมด ${state.pets.size} ตัว" else null,
        isTabScreen = true,
        snackbarHostState = snackbar,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPet,
                containerColor = PetCareTheme.colors.amber,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) { Icon(Icons.Outlined.Add, contentDescription = "เพิ่มสัตว์เลี้ยง") }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@PetCareScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.pets.isNotEmpty() || state.query.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("ค้นหาชื่อ ชนิด หรือสายพันธุ์") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.pets.isEmpty()) {
                item {
                    if (state.query.isBlank()) {
                        EmptyState(
                            icon = Icons.Outlined.Pets,
                            title = "ยังไม่มีสัตว์เลี้ยง",
                            message = "กดปุ่ม + เพื่อเพิ่มสัตว์เลี้ยงตัวแรกของคุณ",
                            actionLabel = "เพิ่มสัตว์เลี้ยง",
                            onAction = onAddPet,
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "ไม่พบผลลัพธ์",
                            message = "ไม่มีสัตว์เลี้ยงที่ตรงกับ \"${state.query}\"",
                        )
                    }
                }
            }

            items(state.pets, key = { it.pet.id }) { item ->
                PetRow(
                    item = item,
                    onClick = { onOpenPet(item.pet.id) },
                    onEdit = { onEditPet(item.pet.id) },
                    onDelete = { pendingDelete = item.pet },
                )
            }
        }
    }

    pendingDelete?.let { pet ->
        ConfirmDialog(
            title = "ลบ ${pet.name}?",
            message = "ข้อมูลสัตว์เลี้ยงและประวัตินัดหมาย/วัคซีนทั้งหมดของ ${pet.name} จะถูกลบถาวร",
            confirmLabel = "ลบ",
            onConfirm = { viewModel.deletePet(pet) },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun PetRow(
    item: PetListItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val pet = item.pet
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            PetAvatar(pet.species, pet.avatarColor, size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pet.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(pet.species, pet.breed).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(if (pet.birthday.isBlank()) "ไม่ทราบอายุ" else "อายุ ${DateUtils.ageLabel(pet.birthday)}")
                        if (item.pendingCount > 0) append(" · รอ ${item.pendingCount} นัด")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.overdueCount > 0) {
                    Text(
                        "เลยกำหนด ${item.overdueCount} นัด",
                        style = MaterialTheme.typography.labelMedium,
                        color = PetCareTheme.colors.rust,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "ตัวเลือก")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("แก้ไข") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("ลบ", color = PetCareTheme.colors.rust) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = PetCareTheme.colors.rust) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
