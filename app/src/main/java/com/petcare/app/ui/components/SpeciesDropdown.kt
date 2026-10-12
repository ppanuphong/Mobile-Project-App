package com.petcare.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.petcare.app.model.SPECIES_OPTIONS

/**
 * เลือกชนิดสัตว์จาก [SPECIES_OPTIONS] ใช้ทั้งฟอร์มสัตว์เลี้ยงและฟอร์มนัดหมาย
 * ค่าที่บันทึกไว้แล้วแต่ไม่อยู่ในรายการ (ข้อมูลเก่า) ยังแสดงในช่องได้ตามปกติ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesDropdown(
    value: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "ชนิดสัตว์",
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = if (value.isBlank()) "" else "${speciesEmoji(value)} $value",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SPECIES_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${speciesEmoji(option)} $option") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
