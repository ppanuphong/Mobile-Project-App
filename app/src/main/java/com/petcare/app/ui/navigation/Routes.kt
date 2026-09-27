package com.petcare.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ARG_PET_ID = "petId"
    const val ARG_APPOINTMENT_ID = "appointmentId"

    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val PETS = "pets"
    const val APPOINTMENTS = "appointments"
    const val NOTIFICATIONS = "notifications"

    const val PET_DETAIL = "pet/{$ARG_PET_ID}"
    const val PET_FORM = "pet_form?$ARG_PET_ID={$ARG_PET_ID}"
    const val APPOINTMENT_FORM = "appointment_form?$ARG_APPOINTMENT_ID={$ARG_APPOINTMENT_ID}&$ARG_PET_ID={$ARG_PET_ID}"

    fun petDetail(petId: String) = "pet/$petId"
    fun petForm(petId: String? = null) = if (petId == null) "pet_form" else "pet_form?$ARG_PET_ID=$petId"
    fun appointmentForm(appointmentId: String? = null, petId: String? = null) = buildString {
        append("appointment_form")
        val params = listOfNotNull(
            appointmentId?.let { "$ARG_APPOINTMENT_ID=$it" },
            petId?.let { "$ARG_PET_ID=$it" },
        )
        if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
    }
}

/** 4 แท็บของ Bottom Navigation */
enum class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "หน้าหลัก", Icons.Filled.Home, Icons.Outlined.Home),
    PETS(Routes.PETS, "สัตว์เลี้ยง", Icons.Filled.Pets, Icons.Outlined.Pets),
    APPOINTMENTS(Routes.APPOINTMENTS, "นัดหมาย", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    NOTIFICATIONS(Routes.NOTIFICATIONS, "แจ้งเตือน", Icons.Filled.Notifications, Icons.Outlined.Notifications),
}
