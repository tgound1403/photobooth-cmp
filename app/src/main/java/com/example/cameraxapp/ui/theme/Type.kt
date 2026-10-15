package com.example.cameraxapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.tgound.cameraxapp.R

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/**
 * Inter, downloaded through Google Fonts. If the download is unavailable the platform
 * default font is used instead.
 */
private val InterFamily = FontFamily(
    Font(GoogleFont("Inter"), googleFontsProvider, FontWeight.Normal),
    Font(GoogleFont("Inter"), googleFontsProvider, FontWeight.Medium),
    Font(GoogleFont("Inter"), googleFontsProvider, FontWeight.SemiBold)
)

// The app uses exactly four text styles.
private val DisplayStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.3).sp
)

private val TitleStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp
)

private val BodyStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
)

private val LabelStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp
)

/** Material 3 typography mapped onto the four app styles. */
val AppTypography = Typography(
    displayLarge = DisplayStyle,
    displayMedium = DisplayStyle,
    displaySmall = DisplayStyle,
    headlineLarge = DisplayStyle,
    headlineMedium = DisplayStyle,
    headlineSmall = DisplayStyle,
    titleLarge = TitleStyle,
    titleMedium = TitleStyle,
    titleSmall = TitleStyle,
    bodyLarge = BodyStyle,
    bodyMedium = BodyStyle,
    bodySmall = BodyStyle,
    labelLarge = LabelStyle,
    labelMedium = LabelStyle,
    labelSmall = LabelStyle
)
