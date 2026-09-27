package com.petcare.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.petcare.app.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** ช่องแบบอ่านอย่างเดียวที่กดแล้วเปิด dialog */
@Composable
private fun ClickableField(
    value: String,
    label: String,
    icon: ImageVector,
    error: String?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(icon, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // ชั้นโปร่งใสรับการแตะแทน TextField
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}

/**
 * เลือกวันที่ ค่าเข้า-ออกเป็น "yyyy-MM-dd" แต่แสดงผลเป็นวันที่แบบไทย
 * @param allowFuture false = ห้ามเลือกวันในอนาคต (ใช้กับวันเกิด)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    allowFuture: Boolean = true,
    placeholder: String = "เลือกวันที่",
) {
    var open by remember { mutableStateOf(false) }
    ClickableField(
        value = if (value.isBlank()) placeholder else DateUtils.toThaiDateWithDay(value),
        label = label,
        icon = Icons.Outlined.CalendarMonth,
        error = error,
        modifier = modifier,
        onClick = { open = true },
    )
    if (open) {
        val initial = DateUtils.parseDate(value)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = allowFuture || utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onValueChange(DateUtils.formatDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    open = false
                }) { Text("ตกลง") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("ยกเลิก") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** เลือกเวลา ค่าเข้า-ออกเป็น "HH:mm" */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    ClickableField(
        value = if (value.isBlank()) "ไม่ระบุ" else "$value น.",
        label = label,
        icon = Icons.Outlined.Schedule,
        error = error,
        modifier = modifier,
        onClick = { open = true },
    )
    if (open) {
        val initial = DateUtils.parseTime(value) ?: LocalTime.of(10, 0)
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(DateUtils.formatTime(LocalTime.of(state.hour, state.minute)))
                    open = false
                }) { Text("ตกลง") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("ยกเลิก") } },
        )
    }
}
