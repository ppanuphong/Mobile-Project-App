package com.petcare.app.ui.screens.login

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.petcare.app.model.Breed
import com.petcare.app.model.PetCategory
import com.petcare.app.ui.components.AvatarColorPicker
import com.petcare.app.ui.components.BirthdayField
import com.petcare.app.ui.components.PetPhotoPicker
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.viewmodel.PetPickerState

/**
 * เลือกสัตว์เลี้ยงตัวแรกตอนสมัคร: หมวดหมู่ → สายพันธุ์ (จาก API) → ตั้งชื่อ
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetPickerSection(
    state: PetPickerState,
    petNameError: String?,
    petBirthdayError: String?,
    onRetryCategories: () -> Unit,
    onSelectCategory: (PetCategory) -> Unit,
    onRetryBreeds: () -> Unit,
    onBreedQueryChange: (String) -> Unit,
    onSelectBreed: (Breed) -> Unit,
    onPetNameChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onAvatarColorChange: (Long) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onClear: () -> Unit,
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("สัตว์เลี้ยงของคุณ", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "ไม่บังคับ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "เลือกประเภทสัตว์ แล้วเลือกสายพันธุ์จากข้อมูลที่ดึงมาจาก API",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // หมวดหมู่
        when {
            state.categoriesLoading -> LoadingRow("กำลังโหลดประเภทสัตว์เลี้ยง…")
            state.categoriesError != null -> ErrorRow(state.categoriesError, onRetryCategories)
            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory?.id == category.id,
                        onClick = { onSelectCategory(category) },
                        label = { Text("${category.emoji} ${category.name}") },
                    )
                }
            }
        }

        val category = state.selectedCategory ?: return@Column

        // สายพันธุ์
        when {
            state.breedsLoading -> LoadingRow("กำลังโหลดสายพันธุ์${category.name}จาก ${category.source.label}…")
            state.breedsError != null -> ErrorRow(state.breedsError, onRetryBreeds)
            else -> {
                BreedField(category, state, onClick = { sheetOpen = true })
                Text(
                    "ข้อมูล ${state.breeds.size} สายพันธุ์ จาก ${category.source.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ข้อมูลเดียวกับหน้า "เพิ่มสัตว์เลี้ยง": รูป สีประจำตัว ชื่อ วันเกิด
        PetPhotoPicker(
            species = state.selectedBreed?.species ?: category.name,
            color = state.avatarColor,
            photo = state.photo,
            isProcessing = state.isProcessingPhoto,
            onPhotoPicked = onPhotoPicked,
            onRemove = onRemovePhoto,
        )
        AvatarColorPicker(selected = state.avatarColor, onSelect = onAvatarColorChange)
        OutlinedTextField(
            value = state.petName,
            onValueChange = onPetNameChange,
            label = { Text("ชื่อ${category.name}ของคุณ *") },
            isError = petNameError != null,
            supportingText = petNameError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        BirthdayField(
            value = state.birthday,
            onValueChange = onBirthdayChange,
            error = petBirthdayError,
        )
        TextButton(onClick = onClear) { Text("ไม่เพิ่มตอนนี้") }
    }

    val category = state.selectedCategory
    if (sheetOpen && category != null) {
        BreedPickerSheet(
            category = category,
            state = state,
            onQueryChange = onBreedQueryChange,
            onSelect = {
                onSelectBreed(it)
                sheetOpen = false
            },
            onDismiss = { sheetOpen = false },
        )
    }
}

/** ช่องแสดงสายพันธุ์ที่เลือก กดแล้วเปิดรายการให้เลือก */
@Composable
private fun BreedField(category: PetCategory, state: PetPickerState, onClick: () -> Unit) {
    val breed = state.selectedBreed
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BreedThumbnail(category, state.breedImageUrl, size = if (breed == null) 40 else 64)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "สายพันธุ์",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    breed?.name ?: "แตะเพื่อเลือกสายพันธุ์",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (breed == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (breed != null && breed.subtitle.isNotBlank()) {
                    Text(
                        breed.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
    }
}

/** รูปสายพันธุ์ (สุนัขมีรูปจาก Dog CEO) ถ้าไม่มีรูปแสดงอีโมจิของหมวด */
@Composable
private fun BreedThumbnail(category: PetCategory, imageUrl: String?, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PetCareTheme.colors.amberContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(category.emoji, fontSize = (size * 0.45f).sp)
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "รูปตัวอย่างสายพันธุ์",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreedPickerSheet(
    category: PetCategory,
    state: PetPickerState,
    onQueryChange: (String) -> Unit,
    onSelect: (Breed) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "${category.emoji} เลือกสายพันธุ์${category.name}",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "${state.breeds.size} สายพันธุ์ · ข้อมูลจาก ${category.source.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.breedQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("ค้นหาสายพันธุ์") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
        val breeds = state.visibleBreeds
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.navigationBarsPadding()) {
            if (state.breedQuery.isBlank()) {
                item { BreedRow(Breed.UNKNOWN, selected = state.selectedBreed == Breed.UNKNOWN, onSelect) }
                item { HorizontalDivider(Modifier.padding(horizontal = 16.dp)) }
            }
            if (breeds.isEmpty()) {
                item {
                    Text(
                        "ไม่พบสายพันธุ์ \"${state.breedQuery}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(breeds, key = { it.id }) { breed ->
                BreedRow(breed, selected = state.selectedBreed?.id == breed.id, onSelect)
            }
        }
    }
}

@Composable
private fun BreedRow(breed: Breed, selected: Boolean, onSelect: (Breed) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onSelect(breed) },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        headlineContent = { Text(breed.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = breed.subtitle.takeIf { it.isNotBlank() }?.let { { Text(it, maxLines = 1) } },
    )
}

@Composable
private fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: () -> Unit) {
    Surface(
        color = PetCareTheme.colors.rustContainer,
        contentColor = PetCareTheme.colors.onRustContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Pets, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onRetry) { Text("ลองใหม่") }
        }
    }
}
