package com.petcare.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.petcare.app.model.AVATAR_COLORS
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils

/** ช่องฟอร์มที่ใช้ร่วมกันระหว่างหน้า "เพิ่มสัตว์เลี้ยง" และส่วนเลือกสัตว์เลี้ยงตอนสมัครสมาชิก */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarColorPicker(selected: Long, onSelect: (Long) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("สีประจำตัว", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AVATAR_COLORS.forEach { color ->
                val isSelected = color == selected
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onSelect(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) Icon(Icons.Filled.Check, contentDescription = "เลือกแล้ว", tint = Color.White)
                }
            }
        }
    }
}

/**
 * รูปสัตว์เลี้ยง + ปุ่มเลือกรูปจากคลังภาพ / ถ่ายรูป / ลบรูป (ดู [rememberImagePicker])
 */
@Composable
fun PetPhotoPicker(
    species: String,
    color: Long,
    photo: String,
    isProcessing: Boolean,
    onPhotoPicked: (Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val picker = rememberImagePicker(onPhotoPicked)
    val pickFromGallery = picker.pickFromGallery
    val takePhoto = picker.takePhoto

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            PetAvatar(
                species = species,
                color = color,
                photo = photo,
                size = size,
                modifier = Modifier.clickable(enabled = !isProcessing, onClick = pickFromGallery),
            )
            if (isProcessing) {
                Box(
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = pickFromGallery, enabled = !isProcessing) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("เลือกรูป")
            }
            TextButton(onClick = takePhoto, enabled = !isProcessing) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("ถ่ายรูป")
            }
            if (photo.isNotBlank()) {
                TextButton(onClick = onRemove, enabled = !isProcessing) {
                    Text("ลบรูป", color = PetCareTheme.colors.rust)
                }
            }
        }
    }
}

/** วันเกิด (ไม่ทราบได้) + แสดงอายุ + ปุ่มล้าง */
@Composable
fun BirthdayField(value: String, onValueChange: (String) -> Unit, error: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        DatePickerField(
            value = value,
            onValueChange = onValueChange,
            label = "วันเกิด",
            placeholder = "ไม่ทราบ",
            error = error,
            allowFuture = false,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isNotBlank()) {
            Text(
                "อายุ ${DateUtils.ageLabel(value)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onValueChange("") }) { Text("ล้างวันเกิด") }
        }
    }
}
