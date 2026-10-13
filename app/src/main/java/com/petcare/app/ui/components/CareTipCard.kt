package com.petcare.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petcare.app.model.CareTip
import com.petcare.app.ui.theme.PetCareTheme
import kotlinx.coroutines.delay

/** เวลาที่แต่ละเคล็ดลับแสดงก่อนเปลี่ยนเอง */
private const val TIP_INTERVAL_MS = 7_000L

/**
 * การ์ดเคล็ดลับใบเดียวที่เปลี่ยนเองทุก 7 วินาที
 * แตะการ์ดหรือกดลูกศรเพื่อดูใบถัดไป/ก่อนหน้า (จับเวลาใหม่ทุกครั้งที่เปลี่ยน)
 */
@Composable
fun CareTipCard(tips: List<CareTip>, modifier: Modifier = Modifier) {
    if (tips.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = index.mod(tips.size)

    LaunchedEffect(current, tips.size) {
        if (tips.size > 1) {
            delay(TIP_INTERVAL_MS)
            index = (current + 1) % tips.size
        }
    }

    val c = PetCareTheme.colors
    Card(
        onClick = { index = (current + 1) % tips.size },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = c.amberContainer, contentColor = c.onAmberContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡 เคล็ดลับวันนี้", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (tips.size > 1) {
                    Text(
                        "${current + 1}/${tips.size}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                label = "careTip",
            ) { i ->
                val tip = tips[i.coerceIn(0, tips.lastIndex)]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .padding(top = 8.dp, end = 8.dp),
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(c.amber.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(tip.emoji, fontSize = 24.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            color = c.amber.copy(alpha = 0.3f),
                            contentColor = c.onAmberContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                tip.tag,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Text(tip.title, style = MaterialTheme.typography.titleMedium)
                        Text(tip.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (tips.size > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // จุดบอกตำแหน่ง
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        tips.indices.take(10).forEach { i ->
                            Box(
                                Modifier
                                    .size(if (i == current) 8.dp else 6.dp)
                                    .background(
                                        if (i == current) c.amberText else c.onAmberContainer.copy(alpha = 0.3f),
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                    IconButton(onClick = { index = (current - 1 + tips.size) % tips.size }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "เคล็ดลับก่อนหน้า")
                    }
                    IconButton(onClick = { index = (current + 1) % tips.size }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "เคล็ดลับถัดไป")
                    }
                }
            } else {
                Spacer(Modifier.padding(bottom = 8.dp))
            }
        }
    }
}
