package com.example.photogether.ui.onboarding.anim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photogether.ui.theme.AppColors
import com.tgound.photogether.R

/**
 * The app icon, drawn from the launcher foreground. The icon's safe zone is scaled up so the
 * camera fills the frame, the same way it reads on the home screen.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(
        modifier.size(size).clipToBounds().background(AppColors.background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.4f),
        )
    }
}
