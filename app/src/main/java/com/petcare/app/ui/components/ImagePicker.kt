package com.petcare.app.ui.components

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

/** ปุ่มเปิดคลังภาพ / กล้อง ที่ได้จาก [rememberImagePicker] */
class ImagePickerLauncher(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit,
)

/**
 * เลือกรูปจากคลังภาพ (Photo Picker ของระบบ) หรือถ่ายด้วยแอปกล้องผ่าน FileProvider
 * ไม่ต้องขอสิทธิ์ใด ๆ; ได้รูปแล้วเรียก [onPicked] พร้อม Uri
 */
@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicked)
    }
    // เก็บเป็น String เพื่อให้รอดตอนหมุนจอ/ระบบปิดแอประหว่างเปิดกล้อง
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraUri?.let { onPicked(it.toUri()) }
    }
    return ImagePickerLauncher(
        pickFromGallery = {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        takePhoto = {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File.createTempFile("photo_", ".jpg", dir),
            )
            cameraUri = uri.toString()
            try {
                camera.launch(uri)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "ไม่พบแอปกล้องในเครื่องนี้", Toast.LENGTH_SHORT).show()
            }
        },
    )
}
