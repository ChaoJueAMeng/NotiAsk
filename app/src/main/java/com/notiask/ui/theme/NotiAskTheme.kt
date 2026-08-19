package com.notiask.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NotiInk = Color(0xFF1B2740)
val NotiInkMuted = Color(0xFF5B6780)
val NotiAccent = Color(0xFF3A6EA8)
val NotiAccentSoft = Color(0xFF7A6BB5)
val NotiSuccess = Color(0xFF1F8A70)
val NotiSkyStart = Color(0xFFC5DCF6)
val NotiSkyMid = Color(0xFFE6D6F4)
val NotiSkyEnd = Color(0xFFD2F0EA)
val NotiGlassFill = Color(0x59FFFFFF)
val NotiGlassFillStrong = Color(0x73FFFFFF)
val NotiGlassStroke = Color(0xA3FFFFFF)
val NotiGlassStrokeSoft = Color(0x66FFFFFF)

private val colorScheme = lightColorScheme(
    primary = NotiAccent,
    onPrimary = Color.White,
    secondary = NotiAccentSoft,
    onSecondary = Color.White,
    background = Color.Transparent,
    onBackground = NotiInk,
    surface = Color(0xCCF7F4FF),
    onSurface = NotiInk,
    surfaceVariant = Color(0x66FFFFFF),
    onSurfaceVariant = NotiInkMuted,
    outline = NotiGlassStrokeSoft,
    error = Color(0xFFB42318),
)

private val typography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        color = NotiInk,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        color = NotiInk,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = NotiInk,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = NotiInk,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = NotiInk,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = NotiInkMuted,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = NotiInk,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = NotiInk,
    ),
)

@Composable
fun NotiAskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
