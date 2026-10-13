package com.petcare.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.petcare.app.model.Pet
import com.petcare.app.ui.components.ConfirmDialog
import com.petcare.app.ui.components.CoverImage
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.ProfileAvatar
import com.petcare.app.ui.components.rememberPhotoBytes
import com.petcare.app.ui.components.speciesEmoji
import com.petcare.app.ui.theme.NumberStyle
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.ProfileUiState
import com.petcare.app.viewmodel.ProfileViewModel

/** จำนวนสัตว์เลี้ยงที่โชว์ในตาราง (แบบช่องเพื่อนของ Facebook) */
private const val PET_GRID_LIMIT = 6

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onAddPet: () -> Unit,
    onOpenPet: (String) -> Unit,
    onSeeAllPets: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    PetCareScaffold(title = "โปรไฟล์", isTabScreen = true, snackbarHostState = snackbar) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@PetCareScaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { ProfileHeader(state, onEditProfile = onEditProfile, onAddPet = onAddPet) }
            item { SectionDivider() }
            item { AboutSection(state) }
            item { SectionDivider() }
            item {
                PetsSection(
                    pets = state.pets,
                    onOpenPet = onOpenPet,
                    onSeeAll = onSeeAllPets,
                    onAddPet = onAddPet,
                )
            }
            item { SectionDivider() }
            item {
                SettingsSection(
                    attentionCount = state.attentionCount,
                    onOpenNotifications = onOpenNotifications,
                    onResetPassword = { confirmReset = true },
                    onLogout = { confirmLogout = true },
                )
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "เปลี่ยนรหัสผ่าน?",
            message = "ระบบจะส่งลิงก์สำหรับตั้งรหัสผ่านใหม่ไปที่ ${state.user.email}",
            confirmLabel = "ส่งลิงก์",
            onConfirm = viewModel::sendPasswordReset,
            onDismiss = { confirmReset = false },
            destructive = false,
        )
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

/** รูปหน้าปก + รูปโปรไฟล์ลอยทับ + ชื่อ + แนะนำตัว + ปุ่ม (เลย์เอาต์แบบ Facebook) */
@Composable
private fun ProfileHeader(state: ProfileUiState, onEditProfile: () -> Unit, onAddPet: () -> Unit) {
    val user = state.user
    Column {
    Box {
        CoverImage(user.coverPhoto, height = 190.dp)
        ProfileAvatar(
            name = user.name,
            photo = user.photo,
            size = 128.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = 64.dp),
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(user.name.ifBlank { "ผู้ใช้" }, style = MaterialTheme.typography.headlineMedium)
        if (user.bio.isNotBlank()) {
            Text(user.bio, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            buildString {
                append("สัตว์เลี้ยง ${state.pets.size} ตัว")
                append(" · นัดทั้งหมด ${state.appointmentCount}")
                if (state.underTreatmentCount > 0) append(" · กำลังรักษา ${state.underTreatmentCount}")
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddPet, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("เพิ่มสัตว์เลี้ยง")
            }
            OutlinedButton(onClick = onEditProfile, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("แก้ไขโปรไฟล์")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    }
}

/** แถบคั่นหนาแบบ Facebook */
@Composable
private fun SectionDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
private fun AboutSection(state: ProfileUiState) {
    val user = state.user
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("รายละเอียด", style = MaterialTheme.typography.titleLarge)
        if (user.location.isNotBlank()) AboutRow(Icons.Outlined.Place, "อาศัยอยู่ที่ ", user.location)
        if (user.phone.isNotBlank()) AboutRow(Icons.Outlined.Phone, "", user.phone)
        if (user.email.isNotBlank()) AboutRow(Icons.Outlined.Email, "", user.email)
        state.memberSince?.let { AboutRow(Icons.Outlined.CalendarMonth, "เป็นสมาชิกตั้งแต่ ", it) }

        // สถิติแบบตัวเลขใหญ่
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("สัตว์เลี้ยง", state.pets.size, Modifier.weight(1f))
            StatTile("นัดทั้งหมด", state.appointmentCount, Modifier.weight(1f))
            StatTile("เสร็จแล้ว", state.doneCount, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, prefix: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            buildString {
                append(prefix)
                append(value)
            },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value.toString(), style = NumberStyle, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** ตารางสัตว์เลี้ยง 3 คอลัมน์ (แบบช่องเพื่อนของ Facebook) */
@Composable
private fun PetsSection(
    pets: List<Pet>,
    onOpenPet: (String) -> Unit,
    onSeeAll: () -> Unit,
    onAddPet: () -> Unit,
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("สัตว์เลี้ยง", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${pets.size} ตัว",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pets.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("ดูทั้งหมด") }
        }
        if (pets.isEmpty()) {
            OutlinedButton(onClick = onAddPet, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("เพิ่มสัตว์เลี้ยงตัวแรก")
            }
        }
        pets.take(PET_GRID_LIMIT).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { pet -> PetTile(pet, onClick = { onOpenPet(pet.id) }, modifier = Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PetTile(pet: Pet, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bytes = rememberPhotoBytes(pet.photo)
    Column(modifier) {
        Card(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(pet.avatarColor)),
                contentAlignment = Alignment.Center,
            ) {
                if (bytes != null) {
                    AsyncImage(
                        model = bytes,
                        contentDescription = pet.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(speciesEmoji(pet.species), fontSize = 40.sp)
                }
            }
        }
        Text(
            pet.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (pet.breed.isNotBlank() || pet.species.isNotBlank()) {
            Text(
                pet.breed.ifBlank { pet.species },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    attentionCount: Int,
    onOpenNotifications: () -> Unit,
    onResetPassword: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            "การตั้งค่า",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        MenuRow(
            icon = Icons.Outlined.Notifications,
            title = "การแจ้งเตือน",
            subtitle = "นัดที่ถึงเวลาเตือน เลยกำหนด และตั้งค่าการแจ้งเตือน",
            badge = attentionCount,
            onClick = onOpenNotifications,
        )
        MenuRow(
            icon = Icons.Outlined.Lock,
            title = "เปลี่ยนรหัสผ่าน",
            subtitle = "ส่งลิงก์ตั้งรหัสผ่านใหม่ทางอีเมล",
            onClick = onResetPassword,
        )
        MenuRow(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = "ออกจากระบบ",
            tint = PetCareTheme.colors.rust,
            showArrow = false,
            onClick = onLogout,
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    badge: Int = 0,
    tint: Color = Color.Unspecified,
    showArrow: Boolean = true,
) {
    val contentColor = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            leadingContent = {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = contentColor) }
            },
            headlineContent = { Text(title, color = contentColor) },
            supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (badge > 0) {
                        Badge(containerColor = PetCareTheme.colors.rust) {
                            Text(badge.toString(), textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    if (showArrow) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}
