package com.petcare.app.ui.screens.login

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petcare.app.ui.components.AvatarColorPicker
import com.petcare.app.ui.components.BirthdayField
import com.petcare.app.ui.components.BreedDropdown
import com.petcare.app.ui.components.PetPhotoPicker
import com.petcare.app.ui.components.SpeciesDropdown
import com.petcare.app.viewmodel.PetPickerState

/**
 * เพิ่มสัตว์เลี้ยงตัวแรกตอนสมัคร ใช้ช่องชุดเดียวกับหน้า "เพิ่มสัตว์เลี้ยง":
 * ชนิดสัตว์ (dropdown) → สายพันธุ์ (dropdown ค้นหาได้ จาก API) → รูป สี ชื่อ วันเกิด
 * ยังไม่เลือกชนิดสัตว์ = ไม่เพิ่มสัตว์เลี้ยง
 */
@Composable
fun PetPickerSection(
    state: PetPickerState,
    petNameError: String?,
    petBirthdayError: String?,
    onSpeciesChange: (String) -> Unit,
    onBreedChange: (String) -> Unit,
    onPetNameChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onAvatarColorChange: (Long) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onClear: () -> Unit,
) {
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
            "เลือกชนิดสัตว์เพื่อเพิ่มสัตว์เลี้ยงตัวแรกไปพร้อมกัน รายชื่อสายพันธุ์ดึงมาจาก API",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SpeciesDropdown(
            value = state.species,
            onSelect = onSpeciesChange,
            label = "ชนิดสัตว์",
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.isAddingPet) return@Column

        BreedDropdown(
            value = state.breed,
            species = state.species,
            options = state.breedOptions,
            loading = state.breedsLoading,
            fromApi = state.breedsFromApi,
            onValueChange = onBreedChange,
            modifier = Modifier.fillMaxWidth(),
        )
        PetPhotoPicker(
            species = state.species,
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
            label = { Text("ชื่อสัตว์เลี้ยง *") },
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
}
