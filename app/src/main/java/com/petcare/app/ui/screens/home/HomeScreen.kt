package com.petcare.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.ui.components.AppointmentCard
import com.petcare.app.ui.components.ConfirmDialog
import com.petcare.app.ui.components.EmptyState
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetAvatar
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.SectionHeader
import com.petcare.app.ui.theme.NumberStyle
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.HomeViewModel
import com.petcare.app.viewmodel.PetSummary
import java.time.LocalDate

@Composable
fun HomeScreen(
    onOpenPet: (String) -> Unit,
    onOpenAppointment: (String) -> Unit,
    onAddPet: () -> Unit,
    onAddAppointment: () -> Unit,
    onSeeAllAppointments: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    PetCareScaffold(
        title = if (state.userName.isBlank()) "สวัสดี" else "สวัสดี, ${state.userName}",
        subtitle = "วันนี้ ${DateUtils.toThaiDateWithDay(DateUtils.formatDate(LocalDate.now()))}",
        isTabScreen = true,
        actions = {
            IconButton(onClick = { confirmLogout = true }) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "ออกจากระบบ")
            }
        },
        floatingActionButton = {
            if (state.pets.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddAppointment,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("เพิ่มนัด") },
                    containerColor = PetCareTheme.colors.amber,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
            }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("เลยกำหนด", state.overdue.size, PetCareTheme.colors.rust, PetCareTheme.colors.rustContainer, Modifier.weight(1f))
                    StatTile("ใกล้ถึง", state.dueSoon.size, PetCareTheme.colors.onAmberContainer, PetCareTheme.colors.amberContainer, Modifier.weight(1f))
                    StatTile("เสร็จเดือนนี้", state.doneThisMonth, PetCareTheme.colors.done, PetCareTheme.colors.doneContainer, Modifier.weight(1f))
                }
            }

            if (state.pets.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Pets,
                        title = "ยังไม่มีสัตว์เลี้ยง",
                        message = "เพิ่มสัตว์เลี้ยงตัวแรกเพื่อเริ่มบันทึกวัคซีนและนัดหมอ",
                        actionLabel = "เพิ่มสัตว์เลี้ยง",
                        onAction = onAddPet,
                    )
                }
                return@LazyColumn
            }

            if (state.overdue.isNotEmpty()) {
                item { SectionHeader("เลยกำหนด", count = state.overdue.size, modifier = Modifier.padding(top = 8.dp)) }
                items(state.overdue, key = { "o_" + it.appointment.id }) { item ->
                    AppointmentCard(
                        item = item,
                        onClick = { onOpenAppointment(item.appointment.id) },
                        onToggleDone = { viewModel.markDone(item) },
                    )
                }
            }

            item {
                SectionHeader(
                    "ใกล้ถึงใน 7 วัน",
                    actionLabel = "ดูทั้งหมด",
                    onAction = onSeeAllAppointments,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.dueSoon.isEmpty()) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = PetCareTheme.colors.done)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "ไม่มีนัดในสัปดาห์นี้ พักผ่อนได้เต็มที่",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(state.dueSoon, key = { "s_" + it.appointment.id }) { item ->
                    AppointmentCard(
                        item = item,
                        onClick = { onOpenAppointment(item.appointment.id) },
                        onToggleDone = { viewModel.markDone(item) },
                    )
                }
            }

            item {
                SectionHeader(
                    "สัตว์เลี้ยงของฉัน",
                    count = state.pets.size,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.pets, key = { it.pet.id }) { summary ->
                        PetMiniCard(summary, onClick = { onOpenPet(summary.pet.id) })
                    }
                    item {
                        OutlinedCard(
                            onClick = onAddPet,
                            modifier = Modifier
                                .width(120.dp)
                                .height(156.dp),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("เพิ่ม", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "ออกจากระบบ?",
            message = "การแจ้งเตือนในเครื่องนี้จะหยุดจนกว่าจะเข้าสู่ระบบอีกครั้ง",
            confirmLabel = "ออกจากระบบ",
            onConfirm = onLogout,
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, container: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(value.toString(), style = NumberStyle, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun PetMiniCard(summary: PetSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(156.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PetAvatar(summary.pet.species, summary.pet.avatarColor, photo = summary.pet.photo, size = 44.dp)
            Text(
                summary.pet.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val next = summary.nextAppointment
            when {
                summary.overdueCount > 0 -> Text(
                    "เลยกำหนด ${summary.overdueCount} นัด",
                    style = MaterialTheme.typography.labelMedium,
                    color = PetCareTheme.colors.rust,
                )
                next != null -> Text(
                    "นัดถัดไป ${DateUtils.relativeLabel(next.appointment.date)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                else -> Text(
                    "ไม่มีนัดค้าง",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
