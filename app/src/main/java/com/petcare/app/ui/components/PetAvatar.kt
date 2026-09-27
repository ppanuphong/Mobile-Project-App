package com.petcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** อีโมจิตามชนิดสัตว์ ถ้าไม่รู้จักใช้อุ้งเท้า */
fun speciesEmoji(species: String): String = when {
    species.contains("สุนัข") || species.contains("หมา") -> "🐶"
    species.contains("แมว") -> "🐱"
    species.contains("กระต่าย") -> "🐰"
    species.contains("นก") -> "🐦"
    species.contains("ปลา") -> "🐟"
    species.contains("หนู") || species.contains("แฮมสเตอร์") -> "🐹"
    species.contains("เต่า") -> "🐢"
    else -> "🐾"
}

@Composable
fun PetAvatar(
    species: String,
    color: Long,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = speciesEmoji(species),
            style = TextStyle(fontSize = (size.value * 0.5f).sp),
        )
    }
}
