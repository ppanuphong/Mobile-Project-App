package com.petcare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.petcare.app.R

/*
 * Fraunces และ Manrope ไม่มีอักษรไทย จึงใช้:
 * - หัวข้อภาษาไทย: Noto Serif Thai (เซอริฟ มีหัว)
 * - ตัวเลขใหญ่ / โลโก้: Fraunces
 * - เนื้อหา: IBM Plex Sans Thai (sans อ่านง่าย)
 * ไฟล์ฟอนต์อยู่ใน res/font (สัญญาอนุญาต SIL OFL)
 */

private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Fraunces = FontFamily(
    variable(R.font.fraunces, 400),
    variable(R.font.fraunces, 600),
    variable(R.font.fraunces, 700),
)

val SerifThai = FontFamily(
    variable(R.font.noto_serif_thai, 400),
    variable(R.font.noto_serif_thai, 500),
    variable(R.font.noto_serif_thai, 600),
    variable(R.font.noto_serif_thai, 700),
)

val SansThai = FontFamily(
    Font(R.font.ibm_plex_sans_thai_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_thai_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_thai_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_thai_bold, FontWeight.Bold),
)

private fun heading(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = SerifThai, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = SansThai,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.em,
    )

// อักษรไทยมีสระบน-ล่าง จึงเผื่อ lineHeight มากกว่าค่าเริ่มต้นของ Material
val PetCareTypography = Typography(
    displayLarge = heading(48, 60, FontWeight.Bold),
    displayMedium = heading(40, 52, FontWeight.Bold),
    displaySmall = heading(34, 46),
    headlineLarge = heading(30, 42),
    headlineMedium = heading(26, 38),
    headlineSmall = heading(22, 34),
    titleLarge = heading(20, 30),
    titleMedium = body(16, 26, FontWeight.SemiBold),
    titleSmall = body(14, 22, FontWeight.SemiBold),
    bodyLarge = body(16, 26),
    bodyMedium = body(14, 22),
    bodySmall = body(12, 19),
    labelLarge = body(14, 20, FontWeight.SemiBold),
    labelMedium = body(12, 18, FontWeight.Medium),
    labelSmall = body(11, 16, FontWeight.Medium, 0.02),
)

/** ตัวเลขใหญ่ (เช่น จำนวนนัด) ใช้ Fraunces ให้มีคาแรกเตอร์ */
val NumberStyle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.sp)
