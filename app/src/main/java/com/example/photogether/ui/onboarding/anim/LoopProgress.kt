package com.example.photogether.ui.onboarding.anim

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Frame shown when animations are off: late enough in the loop to show the finished state. */
const val STATIC_FRAME = 0.88f

/**
 * Looping 0..1 clock for an illustration. Runs only while [active] (the page is on screen) and
 * restarts from 0 each time the page is shown. With the system "Remove animations" setting it
 * holds [STATIC_FRAME] instead of moving.
 */
@Composable
fun rememberLoopProgress(active: Boolean, durationMs: Int): State<Float> {
    val context = LocalContext.current
    val animationsOn = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val clock = remember { Animatable(if (animationsOn) 0f else STATIC_FRAME) }
    LaunchedEffect(active, animationsOn) {
        if (!animationsOn) return@LaunchedEffect
        clock.snapTo(0f)
        if (!active) return@LaunchedEffect
        while (true) {
            clock.snapTo(0f)
            clock.animateTo(1f, tween(durationMs, easing = LinearEasing))
        }
    }
    return clock.asState()
}
