package com.petcare.app.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petcare.app.notification.NotificationHelper
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.screens.appointments.AppointmentFormScreen
import com.petcare.app.ui.screens.appointments.AppointmentListScreen
import com.petcare.app.ui.screens.home.HomeScreen
import com.petcare.app.ui.screens.login.LoginScreen
import com.petcare.app.ui.screens.login.RegisterScreen
import com.petcare.app.ui.screens.notifications.NotificationScreen
import com.petcare.app.ui.screens.pets.PetDetailScreen
import com.petcare.app.ui.screens.pets.PetFormScreen
import com.petcare.app.ui.screens.pets.PetListScreen
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.AuthState
import com.petcare.app.viewmodel.SessionViewModel

@Composable
fun AppNavHost(
    openTab: String?,
    onTabOpened: () -> Unit,
    sessionViewModel: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val authState by sessionViewModel.authState.collectAsStateWithLifecycle()
    if (authState == AuthState.CHECKING) {
        LoadingBox()
        return
    }

    // กำหนดครั้งเดียว: ถ้าเปลี่ยน startDestination ภายหลัง NavHost จะรีเซ็ตกราฟทั้งหมด
    val startDestination = remember { if (authState == AuthState.LOGGED_IN) Routes.HOME else Routes.LOGIN }
    val attentionCount by sessionViewModel.attentionCount.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopLevelTab.entries.firstOrNull { it.route == currentRoute }

    // ออกจากระบบแล้วกลับไปหน้า Login และล้าง back stack ทั้งหมด
    LaunchedEffect(authState) {
        if (authState == AuthState.LOGGED_OUT && currentRoute != null && currentRoute != Routes.LOGIN &&
            currentRoute != Routes.REGISTER
        ) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    // ขอสิทธิ์แสดง Notification (Android 13+) ครั้งแรกหลังเข้าสู่ระบบ
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(authState) {
        if (authState == AuthState.LOGGED_IN &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.canPostNotifications(context)
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // เปิดจาก Notification → ไปแท็บแจ้งเตือน
    LaunchedEffect(openTab, authState) {
        if (openTab != null && authState == AuthState.LOGGED_IN) {
            navController.navigateToTab(openTab)
            onTabOpened()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                BottomBar(
                    current = currentTab,
                    notificationBadge = attentionCount > 0,
                    onSelect = { navController.navigateToTab(it.route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        sessionViewModel.onLoggedIn()
                        navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = {
                        sessionViewModel.onLoggedIn()
                        navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                    },
                    onNavigateUp = { navController.navigateUp() },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onOpenPet = { navController.navigate(Routes.petDetail(it)) },
                    onOpenAppointment = { navController.navigate(Routes.appointmentForm(appointmentId = it)) },
                    onAddPet = { navController.navigate(Routes.petForm()) },
                    onAddAppointment = { navController.navigate(Routes.appointmentForm()) },
                    onSeeAllAppointments = { navController.navigateToTab(Routes.APPOINTMENTS) },
                    onLogout = sessionViewModel::logout,
                )
            }
            composable(Routes.PETS) {
                PetListScreen(
                    onOpenPet = { navController.navigate(Routes.petDetail(it)) },
                    onAddPet = { navController.navigate(Routes.petForm()) },
                    onEditPet = { navController.navigate(Routes.petForm(it)) },
                )
            }
            composable(Routes.APPOINTMENTS) {
                AppointmentListScreen(
                    onOpenAppointment = { navController.navigate(Routes.appointmentForm(appointmentId = it)) },
                    onAddAppointment = { navController.navigate(Routes.appointmentForm()) },
                    onAddPet = { navController.navigate(Routes.petForm()) },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationScreen(
                    onOpenAppointment = { navController.navigate(Routes.appointmentForm(appointmentId = it)) },
                )
            }

            composable(
                Routes.PET_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_PET_ID) { type = NavType.StringType }),
            ) {
                PetDetailScreen(
                    onNavigateUp = { navController.navigateUp() },
                    onEdit = { navController.navigate(Routes.petForm(it)) },
                    onAddAppointment = { navController.navigate(Routes.appointmentForm(petId = it)) },
                    onOpenAppointment = { navController.navigate(Routes.appointmentForm(appointmentId = it)) },
                )
            }
            composable(
                Routes.PET_FORM,
                arguments = listOf(optionalStringArg(Routes.ARG_PET_ID)),
            ) {
                PetFormScreen(onDone = { navController.navigateUp() })
            }
            composable(
                Routes.APPOINTMENT_FORM,
                arguments = listOf(optionalStringArg(Routes.ARG_APPOINTMENT_ID), optionalStringArg(Routes.ARG_PET_ID)),
            ) {
                AppointmentFormScreen(
                    onDone = { navController.navigateUp() },
                    onAddPet = { navController.navigate(Routes.petForm()) },
                )
            }
        }
    }
}

private fun optionalStringArg(name: String) = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

/** สลับแท็บแบบเก็บ state ของแต่ละแท็บไว้ */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(
    current: TopLevelTab,
    notificationBadge: Boolean,
    onSelect: (TopLevelTab) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        TopLevelTab.entries.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onSelect(tab) },
                icon = {
                    BadgedBox(badge = { if (tab == TopLevelTab.NOTIFICATIONS && notificationBadge) Badge() }) {
                        Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null)
                    }
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = PetCareTheme.colors.amberContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}
