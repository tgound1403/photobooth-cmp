package com.example.cameraxapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonCyan,
    tertiary = Pink80,
    background = DeepBlack,
    surface = SurfaceBlack,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    // Other default colors to override
)

private val PastelColorScheme = lightColorScheme(
    primary = PastelBlue,
    onPrimary = CharcoalText,
    secondary = PastelPink,
    onSecondary = CharcoalText,
    tertiary = PastelLilac,
    background = CreamWhite,
    onBackground = CharcoalText,
    surface = PastelCream, // or specialized surface color
    onSurface = CharcoalText
)

private val BlackAndWhiteColorScheme = darkColorScheme(
    primary = BWWhite,
    onPrimary = BWBlack,
    secondary = BWWhite,
    onSecondary = BWBlack,
    tertiary = BWDarkGray,
    background = BWBlack,
    onBackground = BWWhite,
    surface = BWBlack,
    onSurface = BWWhite
)

@Composable
fun CameraXAppTheme(
    appTheme: AppTheme = AppTheme.DARK_NEON,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.DARK_NEON -> DarkColorScheme
        AppTheme.KOREAN_PASTEL -> PastelColorScheme
        AppTheme.BLACK_AND_WHITE -> BlackAndWhiteColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = appTheme == AppTheme.KOREAN_PASTEL
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
