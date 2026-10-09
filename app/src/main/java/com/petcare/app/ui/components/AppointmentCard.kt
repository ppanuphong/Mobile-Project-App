package com.petcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petcare.app.model.AppointmentItem
import com.petcare.app.model.AppointmentKind
import com.petcare.app.model.DisplayStatus
import com.petcare.app.model.TreatmentOutcome
import com.petcare.app.ui.theme.Fraunces
import com.petcare.app.ui.theme.colors
import com.petcare.app.util.DateUtils

fun AppointmentKind.icon(): ImageVector = when (this) {
    AppointmentKind.VACCINE -> Icons.Outlined.Vaccines
    AppointmentKind.CHECKUP -> Icons.Outlined.MedicalServices
    AppointmentKind.TREATMENT -> Icons.Outlined.Healing
    AppointmentKind.MEDICATION -> Icons.Outlined.Medication
    AppointmentKind.GROOMING -> Icons.Outlined.ContentCut
    AppointmentKind.OTHER -> Icons.Outlined.Event
}

@Composable
fun StatusChip(status: DisplayStatus, modifier: Modifier = Modifier) {
    val colors = status.colors()
    Surface(modifier = modifier, color = colors.container, contentColor = colors.content, shape = RoundedCornerShape(50)) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

/**
 * การ์ดนัดหมาย: บล็อกวันที่ทางซ้าย, รายละเอียดตรงกลาง, ปุ่มทำเครื่องหมายเสร็จทางขวา
 * แถบสีด้านซ้ายบอกสถานะ (สนิม = เลยกำหนด, อำพัน = ใกล้ถึง)
 */
@Composable
fun AppointmentCard(
    item: AppointmentItem,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
    showPet: Boolean = true,
) {
    val appt = item.appointment
    val statusColors = item.status.colors()
    val date = DateUtils.parseDate(appt.date)
    val isDone = item.status == DisplayStatus.DONE

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusColors.accent),
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // บล็อกวันที่
                Column(
                    Modifier
                        .width(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColors.container)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = date?.dayOfMonth?.toString() ?: "-",
                        fontFamily = Fraunces,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        color = statusColors.content,
                    )
                    Text(
                        text = date?.let { DateUtils.thaiMonthShort(it) }.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColors.content,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            appt.kindEnum.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = buildString {
                                append(appt.kindEnum.label)
                                if (showPet && item.pet != null) append(" · ${item.pet.name}")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = appt.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    )
                    // นัดที่บันทึกผลแล้ว แสดงผลวินิจฉัยแทนเวลา/คลินิก
                    val diagnosis = appt.result?.diagnosis?.takeIf { isDone && it.isNotBlank() }
                    val details = diagnosis?.let { "ผล: $it" } ?: listOfNotNull(
                        appt.time.takeIf { it.isNotBlank() }?.let { "$it น." },
                        appt.clinic.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (details.isNotBlank()) {
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(item.status)
                        appt.result?.outcomeEnum?.takeIf { it != TreatmentOutcome.NORMAL }?.let {
                            Spacer(Modifier.width(6.dp))
                            OutcomeChip(it)
                        }
                        if (!isDone) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                DateUtils.relativeLabel(appt.date),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColors.text,
                            )
                        }
                    }
                }
                IconButton(onClick = onToggleDone) {
                    Icon(
                        imageVector = if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (isDone) "ยกเลิกเสร็จแล้ว" else "ทำเครื่องหมายว่าเสร็จ",
                        tint = if (isDone) statusColors.text else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
