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
 * Space Mono, downloaded through Google Fonts. If the download is unavailable the platform
 * default font is used instead.
 */
private val SpaceMonoFamily = FontFamily(
    Font(GoogleFont("Space Mono"), googleFontsProvider, FontWeight.Normal),
    Font(GoogleFont("Space Mono"), googleFontsProvider, FontWeight.Bold)
)

/** Archivo Black, downloaded through Google Fonts. Only a regular weight exists. */
private val ArchivoBlackFamily = FontFamily(
    Font(GoogleFont("Archivo Black"), googleFontsProvider, FontWeight.Normal)
)

/** The app's text styles. Use these directly where Material typography has no slot (e.g. micro). */
object BinsooType {
    /** Archivo Black 56sp. Write the text in caps yourself with .uppercase(). */
    val display = TextStyle(
        fontFamily = ArchivoBlackFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp
    )

    /** Space Mono Bold 18sp. */
    val title = TextStyle(
        fontFamily = SpaceMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    /** Space Mono 13sp. */
    val body = TextStyle(
        fontFamily = SpaceMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    )

    /** Space Mono 11sp, usually uppercase. */
    val label = TextStyle(
        fontFamily = SpaceMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )

    /** Space Mono 9sp. Photo metadata ("SHOT ON / LENS / DATE"). */
    val micro = TextStyle(
        fontFamily = SpaceMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp
    )
}

/** Material 3 typography mapped onto the Binsoo styles. */
val AppTypography = Typography(
    displayLarge = BinsooType.display,
    displayMedium = BinsooType.display,
    displaySmall = BinsooType.display,
    // Dialog titles and similar use headlineSmall. 56sp is too large there, so it gets 32sp.
    headlineLarge = BinsooType.display,
    headlineMedium = BinsooType.display,
    headlineSmall = BinsooType.display.copy(fontSize = 32.sp, lineHeight = 38.sp),
    titleLarge = BinsooType.title,
    titleMedium = BinsooType.title,
    titleSmall = BinsooType.title,
    bodyLarge = BinsooType.body,
    bodyMedium = BinsooType.body,
    bodySmall = BinsooType.body,
    labelLarge = BinsooType.label,
    labelMedium = BinsooType.label,
    labelSmall = BinsooType.label
)
