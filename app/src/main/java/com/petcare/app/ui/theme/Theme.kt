package com.petcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.petcare.app.model.DisplayStatus

private val LightColors = lightColorScheme(
    primary = Teal900,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Color(0xFF05302E),
    secondary = Amber500,
    onSecondary = Color(0xFF2E1F00),
    secondaryContainer = Amber100,
    onSecondaryContainer = Color(0xFF4A3100),
    tertiary = Rust600,
    onTertiary = Color.White,
    tertiaryContainer = Rust100,
    onTertiaryContainer = Color(0xFF4A1706),
    error = Rust600,
    onError = Color.White,
    errorContainer = Rust100,
    onErrorContainer = Color(0xFF4A1706),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = Teal900,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F8F7),
    surfaceContainer = Color(0xFFF1F5F3),
    surfaceContainerHigh = Color(0xFFE9EFEC),
    surfaceContainerHighest = Color(0xFFE2E9E6),
)

private val DarkColors = darkColorScheme(
    primary = Teal200,
    onPrimary = Color(0xFF00312E),
    primaryContainer = Teal800,
    onPrimaryContainer = Teal100,
    secondary = Amber300,
    onSecondary = Color(0xFF3E2A00),
    secondaryContainer = Color(0xFF5C4210),
    onSecondaryContainer = Amber100,
    tertiary = Rust300,
    onTertiary = Color(0xFF4A1706),
    tertiaryContainer = Color(0xFF6E2E16),
    onTertiaryContainer = Rust100,
    error = Rust300,
    onError = Color(0xFF4A1706),
    errorContainer = Color(0xFF6E2E16),
    onErrorContainer = Rust100,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = Teal200,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = Color(0xFF0B100F),
    surfaceContainerLow = Color(0xFF141B19),
    surfaceContainer = Color(0xFF18201E),
    surfaceContainerHigh = Color(0xFF1F2826),
    surfaceContainerHighest = Color(0xFF29332F),
)

/** สีเพิ่มเติมนอก Material: แถบบน และสีของแต่ละสถานะนัด */
@Immutable
data class PetCareColors(
    val topBar: Color,
    val onTopBar: Color,
    val onTopBarMuted: Color,
    val amber: Color,
    /** สีอำพันเข้มสำหรับตัวอักษรบนพื้นสว่าง (อำพันปกติอ่านยากบนพื้นขาว) */
    val amberText: Color,
    val amberContainer: Color,
    val onAmberContainer: Color,
    val rust: Color,
    val rustContainer: Color,
    val onRustContainer: Color,
    val done: Color,
    val doneContainer: Color,
    val onDoneContainer: Color,
    val upcomingContainer: Color,
    val onUpcomingContainer: Color,
)

private val LightExtra = PetCareColors(
    topBar = Teal900,
    onTopBar = Color.White,
    onTopBarMuted = Color(0xCCFFFFFF),
    amber = Amber500,
    amberText = Color(0xFF8A5A00),
    amberContainer = Amber100,
    onAmberContainer = Color(0xFF5A3B00),
    rust = Rust600,
    rustContainer = Rust100,
    onRustContainer = Color(0xFF5A1D08),
    done = Sage600,
    doneContainer = Sage100,
    onDoneContainer = Color(0xFF1D3A25),
    upcomingContainer = Teal100,
    onUpcomingContainer = Color(0xFF05302E),
)

private val DarkExtra = PetCareColors(
    topBar = DarkTopBar,
    onTopBar = Color(0xFFE6F2EF),
    onTopBarMuted = Color(0xB3E6F2EF),
    amber = Amber300,
    amberText = Amber300,
    amberContainer = Color(0xFF4A360E),
    onAmberContainer = Amber100,
    rust = Rust300,
    rustContainer = Color(0xFF5A2512),
    onRustContainer = Rust100,
    done = Sage300,
    doneContainer = Color(0xFF223A2A),
    onDoneContainer = Sage100,
    upcomingContainer = Color(0xFF123F3C),
    onUpcomingContainer = Teal100,
)

val LocalPetCareColors = staticCompositionLocalOf { LightExtra }

@Composable
fun PetCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPetCareColors provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = PetCareTypography,
            content = content,
        )
    }
}

object PetCareTheme {
    val colors: PetCareColors
        @Composable get() = LocalPetCareColors.current
}

/** สีของสถานะ: container/content = ป้าย, accent = แถบข้าง, text = ข้อความบนพื้นการ์ด */
data class StatusColors(val container: Color, val content: Color, val accent: Color, val text: Color)

@Composable
fun DisplayStatus.colors(): StatusColors {
    val c = PetCareTheme.colors
    return when (this) {
        DisplayStatus.OVERDUE -> StatusColors(c.rustContainer, c.onRustContainer, c.rust, c.rust)
        DisplayStatus.DUE_SOON -> StatusColors(c.amberContainer, c.onAmberContainer, c.amber, c.amberText)
        DisplayStatus.UPCOMING -> MaterialTheme.colorScheme.primary.let {
            StatusColors(c.upcomingContainer, c.onUpcomingContainer, it, it)
        }
        DisplayStatus.DONE -> StatusColors(c.doneContainer, c.onDoneContainer, c.done, c.done)
    }
}
