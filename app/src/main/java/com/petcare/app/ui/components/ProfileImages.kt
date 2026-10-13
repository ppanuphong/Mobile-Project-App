package com.petcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.petcare.app.ui.theme.Fraunces
import com.petcare.app.ui.theme.PetCareTheme
import java.util.Base64

/** แปลงรูป Base64 ที่เก็บใน Firestore เป็น bytes ให้ Coil แสดง (null = ไม่มีรูป / ข้อมูลเสีย) */
@Composable
fun rememberPhotoBytes(photo: String): ByteArray? = remember(photo) {
    photo.takeIf { it.isNotBlank() }?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }
}

/** ตัวอักษรแรกของชื่อ ใช้แทนรูปโปรไฟล์ */
fun nameInitial(name: String): String =
    name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

/** รูปโปรไฟล์ผู้ใช้ทรงกลม มีขอบสีพื้นหลัง (ให้ดูลอยทับรูปหน้าปกแบบ Facebook) */
@Composable
fun ProfileAvatar(
    name: String,
    photo: String,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    borderWidth: Dp = 4.dp,
) {
    val bytes = rememberPhotoBytes(photo)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, MaterialTheme.colorScheme.background, CircleShape)
            .background(PetCareTheme.colors.topBar),
        contentAlignment = Alignment.Center,
    ) {
        if (bytes != null) {
            AsyncImage(
                model = bytes,
                contentDescription = "รูปโปรไฟล์",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                nameInitial(name),
                fontFamily = Fraunces,
                fontSize = (size.value * 0.42f).sp,
                color = PetCareTheme.colors.onTopBar,
            )
        }
    }
}

/** รูปหน้าปก ถ้ายังไม่มีรูปใช้พื้นไล่สีเขียวมรกต → อำพัน */
@Composable
fun CoverImage(photo: String, modifier: Modifier = Modifier, height: Dp = 180.dp) {
    val bytes = rememberPhotoBytes(photo)
    val c = PetCareTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.linearGradient(listOf(c.topBar, c.topBar.copy(alpha = 0.85f), c.amber))),
    ) {
        if (bytes != null) {
            AsyncImage(
                model = bytes,
                contentDescription = "รูปหน้าปก",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // ลายอุ้งเท้าจาง ๆ เป็นลายพื้น (อีโมจิสีจริงตัดกับธีม)
            Text(
                "🐾",
                fontSize = 64.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(96.dp)
                    .alpha(0.25f),
            )
        }
    }
}
