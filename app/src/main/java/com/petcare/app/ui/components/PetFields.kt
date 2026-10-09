package com.petcare.app.ui.components

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.petcare.app.model.AVATAR_COLORS
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.util.DateUtils
import java.io.File

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
 * รูปโปรไฟล์ + ปุ่มเลือกรูปจากคลังภาพ / ถ่ายรูป / ลบรูป
 * ไม่ต้องขอสิทธิ์ใด ๆ: Photo Picker ของระบบ และแอปกล้องผ่าน FileProvider
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
    val context = LocalContext.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPhotoPicked)
    }
    // เก็บเป็น String เพื่อให้รอดตอนหมุนจอ/ระบบปิดแอประหว่างเปิดกล้อง
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraUri?.let { onPhotoPicked(it.toUri()) }
    }
    val pickFromGallery = {
        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val takePhoto = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File.createTempFile("pet_", ".jpg", dir),
        )
        cameraUri = uri.toString()
        try {
            camera.launch(uri)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "ไม่พบแอปกล้องในเครื่องนี้", Toast.LENGTH_SHORT).show()
        }
    }

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
