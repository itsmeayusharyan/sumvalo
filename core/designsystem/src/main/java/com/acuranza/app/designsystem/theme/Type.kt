package com.acuranza.app.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val Base = FontFamily.Default

/** Tabular figures so that amounts in lists line up digit-for-digit. */
const val TabularFigures = "tnum"

internal val SumvaloTypography = Typography(
    displayLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-1.0).sp),
    displaySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
)

/** Extra text styles for money, all with tabular figures. */
object AmountStyles {
    val hero = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-1.2).sp, fontFeatureSettings = TabularFigures)
    val large = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp, fontFeatureSettings = TabularFigures)
    val medium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = TabularFigures)
    val small = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TabularFigures)
}

/** Text styles outside the Material scale. */
object SumvaloText {
    /** Section headers: small, uppercase, letter-spaced. Uppercase the string yourself. */
    val sectionLabel = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.12.em)

    /** Large left-aligned screen titles. */
    val screenTitle = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp)
}
