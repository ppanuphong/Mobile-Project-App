package com.petcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.util.Base64

/** อีโมจิตามชนิดสัตว์ ถ้าไม่รู้จักใช้อุ้งเท้า */
fun speciesEmoji(species: String): String = when {
    species.contains("สุนัข") || species.contains("หมา") -> "🐶"
    species.contains("แมว") -> "🐱"
    species.contains("กระต่าย") -> "🐰"
    species.contains("นก") -> "🐦"
    species.contains("ปลา") -> "🐟"
    species.contains("หนู") || species.contains("แฮมสเตอร์") -> "🐹"
    species.contains("เต่า") -> "🐢"
    species.contains("งู") -> "🐍"
    species.contains("กิ้งก่า") || species.contains("ตุ๊กแก") || species.contains("อีกัวน่า") -> "🦎"
    species.contains("เม่น") -> "🦔"
    species.contains("ชินชิล่า") || species.contains("เดกู") -> "🐭"
    species.contains("ชูก้าไกลเดอร์") -> "🐿️"
    species.contains("เฟอร์เร็ต") -> "🦦"
    else -> "🐾"
}

/**
 * รูปโปรไฟล์สัตว์เลี้ยงทรงกลม
 * @param photo รูป JPEG แบบ Base64 จาก Pet.photo; ว่าง = แสดงอีโมจิบนพื้นสีประจำตัว
 */
@Composable
fun PetAvatar(
    species: String,
    color: Long,
    modifier: Modifier = Modifier,
    photo: String = "",
    size: Dp = 48.dp,
) {
    val bytes = remember(photo) {
        photo.takeIf { it.isNotBlank() }?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        if (bytes != null) {
            AsyncImage(
                model = bytes,
                contentDescription = "รูปสัตว์เลี้ยง",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = speciesEmoji(species),
                style = TextStyle(fontSize = (size.value * 0.5f).sp),
            )
        }
    }
}
