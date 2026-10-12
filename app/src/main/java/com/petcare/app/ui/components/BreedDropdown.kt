package com.petcare.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.petcare.app.model.Breed
import com.petcare.app.model.BreedGroup

/** จำกัดจำนวนรายการในเมนูพันธุ์ ให้เลื่อนลื่น (พิมพ์ค้นหาเพื่อแคบลง) */
private const val MAX_BREED_ITEMS = 50

/**
 * ช่องสายพันธุ์ ใช้ทั้งฟอร์มสัตว์เลี้ยงและฟอร์มนัดหมาย
 * พิมพ์ค้นหาได้ทั้งไทย/อังกฤษ เลือกจากรายการ หรือพิมพ์เองถ้าไม่มีในรายการ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedDropdown(
    value: String,
    species: String,
    options: List<Breed>,
    loading: Boolean,
    fromApi: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(value, options) {
        // ถ้าค่าปัจจุบันเป็นรายการที่เลือกไว้แล้ว ให้เปิดเมนูเห็นทั้งหมด ไม่ใช่กรองเหลือตัวเดียว
        val query = if (options.any { it.label == value }) "" else value
        options.filter { it.matches(query) }.take(MAX_BREED_ITEMS)
    }
    val group = BreedGroup.fromSpecies(species)
    val hint = when {
        loading -> null
        fromApi && group == BreedGroup.DOG -> "ข้อมูลพันธุ์จาก dog.ceo"
        fromApi && group == BreedGroup.CAT -> "ข้อมูลพันธุ์จาก catfact.ninja"
        group == BreedGroup.DOG || group == BreedGroup.CAT -> "ออฟไลน์ – แสดงรายการพื้นฐาน"
        group == null && species.isNotBlank() -> "ไม่มีรายการสำหรับชนิดนี้ พิมพ์สายพันธุ์เองได้"
        else -> null
    }
    val showMenu = expanded && filtered.isNotEmpty()

    ExposedDropdownMenuBox(expanded = showMenu, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text("สายพันธุ์") },
            placeholder = { Text("พิมพ์ค้นหา เช่น ปั๊ก, Pug") },
            trailingIcon = {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else ExposedDropdownMenuDefaults.TrailingIcon(showMenu)
            },
            supportingText = hint?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { expanded = false }) {
            filtered.forEach { breed ->
                DropdownMenuItem(
                    text = { Text(breed.label) },
                    onClick = {
                        onValueChange(breed.label)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
