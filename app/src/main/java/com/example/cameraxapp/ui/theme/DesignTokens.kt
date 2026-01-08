package com.example.cameraxapp.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for consistent spacing throughout the app
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val xxxl: Dp = 64.dp
    
    // Common spacing values
    val extraSmall: Dp = xs
    val small: Dp = sm
    val medium: Dp = md
    val large: Dp = lg
    val extraLarge: Dp = xl
    
    // Specific spacing for common use cases
    val cardPadding: Dp = md
    val screenPadding: Dp = md
    val itemSpacing: Dp = sm
    val sectionSpacing: Dp = lg
}

/**
 * Design tokens for consistent corner radius throughout the app
 */
object CornerRadius {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    
    // Common corner radius values
    val small: Dp = sm
    val medium: Dp = md
    val large: Dp = lg
    val extraLarge: Dp = xl
    
    // Specific corner radius for common components
    val button: Dp = xl
    val card: Dp = lg
    val image: Dp = md
    val dialog: Dp = lg
}

/**
 * Design tokens for consistent icon sizes throughout the app
 */
object IconSize {
    val xs: Dp = 16.dp
    val sm: Dp = 20.dp
    val md: Dp = 24.dp
    val lg: Dp = 32.dp
    val xl: Dp = 40.dp
    val xxl: Dp = 48.dp
    val xxxl: Dp = 64.dp
    
    // Common icon sizes
    val small: Dp = sm
    val medium: Dp = md
    val large: Dp = lg
    val extraLarge: Dp = xl
    
    // Specific icon sizes for common use cases
    val button: Dp = md
    val appBar: Dp = md
    val listItem: Dp = lg
    val emptyState: Dp = xxxl
    val errorState: Dp = xxxl
}

/**
 * Design tokens for consistent border widths throughout the app
 */
object BorderWidth {
    val none: Dp = 0.dp
    val thin: Dp = 1.dp
    val medium: Dp = 2.dp
    val thick: Dp = 3.dp
    val extraThick: Dp = 4.dp
    
    // Common border widths
    val default: Dp = thin
    val selected: Dp = thick
    val focused: Dp = medium
}

/**
 * Design tokens for consistent elevation/shadow values
 */
object Elevation {
    val none: Dp = 0.dp
    val sm: Dp = 2.dp
    val md: Dp = 4.dp
    val lg: Dp = 8.dp
    val xl: Dp = 16.dp
    
    // Common elevation values
    val card: Dp = md
    val button: Dp = sm
    val dialog: Dp = xl
    val appBar: Dp = md
}
