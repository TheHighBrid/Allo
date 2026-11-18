package com.kenza.callsim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// iOS system palette used across the call UI - perfectly matching iOS 17+ Phone app
object IOSColors {
    // Primary action colors
    val Green = Color(0xFF34C759)        // Answer button, active states
    val Red = Color(0xFFFF3B30)          // Decline/End call button
    val Blue = Color(0xFF0A84FF)         // Links, accent color
    val Orange = Color(0xFFFF9F0A)      // Warning states
    
    // Background colors
    val GroupedBackground = Color(0xFF000000)           // Primary background
    val SecondaryBackground = Color(0xFF1C1C1E)        // Card backgrounds
    val TertiaryBackground = Color(0xFF2C2C2E)         // Secondary surfaces
    val QuaternaryBackground = Color(0xFF3A3A3C)       // Tertiary surfaces
    
    // Call screen gradient colors
    val CallScreenTop = Color(0xFF1C1C1E)
    val CallScreenMid = Color(0xFF0B2933)
    val CallScreenBottom = Color(0xFF000000)
    
    // Incoming call screen gradient (iOS 26 style)
    val IncomingCallTop = Color(0xFF0B2933)
    val IncomingCallMid = Color(0xFF0C5755)
    val IncomingCallBottom = Color(0xFF0A3F49)
    
    // Label colors
    val Label = Color(0xFFFFFFFF)              // Primary text
    val SecondaryLabel = Color(0xCCFFFFFF)      // Secondary text (0.8 opacity)
    val TertiaryLabel = Color(0x99FFFFFF)       // Tertiary text (0.6 opacity)
    val QuaternaryLabel = Color(0x66FFFFFF)    // Quaternary text (0.4 opacity)
    
    // Control colors
    val ControlGrey = Color(0xFF3A3A3C)         // Idle round button
    val ControlGreyActive = Color(0xFFE9E9EA)   // Toggled-on button (white-ish)
    val ControlGreyDisabled = Color(0xFF3A3A3C).copy(alpha = 0.4f)
    
    // Keypad colors
    val KeypadBackground = Color(0xFF2C2C2E)
    val KeypadDigit = Color(0xFFFFFFFF)
    val KeypadLetter = Color(0xCCFFFFFF)
    
    // Separator colors
    val Separator = Color(0xFF38383A)
    val SeparatorOpaque = Color(0x66FFFFFF)     // For light mode or translucent separators
    
    // Glass effect colors (iOS 26)
    val GlassFill = Color.White.copy(alpha = 0.11f)
    val GlassStroke = Color.White.copy(alpha = 0.18f)
    val GlassFillStrong = Color.White.copy(alpha = 0.20f)
    
    // Status colors
    val StatusGreen = Color(0xFF30D158)         // Connected status
    val StatusYellow = Color(0xFFFFD60A)        // Connecting status
    val StatusRed = Color(0xFFFF453A)           // Error status
    
    // System colors
    val SystemBlue = Color(0xFF007AFF)
    val SystemGreen = Color(0xFF34C759)
    val SystemIndigo = Color(0xFF5856D6)
    val SystemOrange = Color(0xFFFF9500)
    val SystemPink = Color(0xFFFF2D55)
    val SystemPurple = Color(0xFFAF52DE)
    val SystemRed = Color(0xFFFF3B30)
    val SystemTeal = Color(0xFF5AC8FA)
    val SystemYellow = Color(0xFFFFCC00)
    
    // Gradient colors for buttons and controls
    val ButtonGradientStart = Color(0xFF4C65D9CC)
    val ButtonGradientMid = Color(0xFF385AB6C8)
    val ButtonGradientEnd = Color(0xFF3457AAB8)
    
    // Shadow colors
    val ShadowColor = Color.Black.copy(alpha = 0.2f)
    val ShadowColorStrong = Color.Black.copy(alpha = 0.4f)
    
    // Avatar gradient colors (iOS-style)
    val avatarColors = listOf(
        Color(0xFF34C759), // Green
        Color(0xFF0A84FF), // Blue
        Color(0xFFFF9500), // Orange
        Color(0xFFFF2D55), // Red
        Color(0xFFAF52DE), // Purple
        Color(0xFF5AC8FA), // Teal
        Color(0xFF5856D6), // Indigo
        Color(0xFFFFD60A), // Yellow
        Color(0xFF30D158), // Brighter Green
        Color(0xFF007AFF), // Brighter Blue
    )
}

// iOS SF Pro font family approximation
val IOSFontFamily = FontFamily.SansSerif

private val DarkColors = darkColorScheme(
    primary = IOSColors.Green,
    secondary = IOSColors.Blue,
    tertiary = IOSColors.SystemTeal,
    background = IOSColors.GroupedBackground,
    surface = IOSColors.SecondaryBackground,
    surfaceVariant = IOSColors.TertiaryBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = IOSColors.SecondaryLabel,
    outline = IOSColors.Separator,
    outlineVariant = IOSColors.Separator.copy(alpha = 0.5f),
)

// iOS-style typography matching SF Pro font metrics
val IOSTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Thin,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = -0.5.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = -0.5.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = -0.4.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = -0.4.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = -0.4.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = -0.4.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = -0.4.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = -0.2.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = -0.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = -0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = -0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = -0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = -0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = IOSFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun KenzaCallTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // The iOS call screen is always dark.
    MaterialTheme(
        colorScheme = DarkColors,
        typography = IOSTypography,
        content = content
    )
}
