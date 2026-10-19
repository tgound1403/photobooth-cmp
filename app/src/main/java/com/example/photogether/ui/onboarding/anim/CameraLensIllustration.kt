package com.example.photogether.ui.onboarding.anim

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Camera body whose lens iris breathes while waiting for permission and flies open once granted. */
@Composable
fun CameraLensIllustration(granted: Boolean, modifier: Modifier = Modifier) {
    val clock = rememberLoopProgress(active = !granted, durationMs = 2600)
    val opened by animateFloatAsState(if (granted) 1f else 0f, tween(700), label = "lensOpen")
    Canvas(modifier.fillMaxWidth().aspectRatio(1.4f)) {
        val strokeW = 2.dp.toPx()
        val corner = CornerRadius(8.dp.toPx())
        val bodyTop = size.height * 0.24f
        val bodyH = size.height * 0.66f
        val bodyLeft = size.width * 0.1f
        val bodyW = size.width * 0.8f
        // Viewfinder bump and flash.
        drawRoundRect(Art.card, Offset(size.width * 0.58f, bodyTop - size.height * 0.1f), Size(size.width * 0.18f, size.height * 0.14f), CornerRadius(3.dp.toPx()))
        drawRoundRect(Art.ink, Offset(size.width * 0.58f, bodyTop - size.height * 0.1f), Size(size.width * 0.18f, size.height * 0.14f), CornerRadius(3.dp.toPx()), style = Stroke(strokeW))
        // Body.
        drawRoundRect(Art.card, Offset(bodyLeft, bodyTop), Size(bodyW, bodyH), corner)
        drawRoundRect(Art.ink, Offset(bodyLeft, bodyTop), Size(bodyW, bodyH), corner, style = Stroke(strokeW * 1.5f))
        drawCircle(Art.yellow, size.width * 0.028f, Offset(bodyLeft + bodyW * 0.14f, bodyTop + bodyH * 0.2f))
        drawCircle(Art.ink, size.width * 0.028f, Offset(bodyLeft + bodyW * 0.14f, bodyTop + bodyH * 0.2f), style = Stroke(strokeW))
        // Lens.
        val c = Offset(size.width / 2f, bodyTop + bodyH * 0.52f)
        val lensR = size.height * 0.27f
        drawCircle(Art.paper, lensR * 1.12f, c)
        drawCircle(Art.ink, lensR * 1.12f, c, style = Stroke(strokeW * 1.5f))
        val breath = 0.2f + 0.05f * sin(clock.value * 2f * PI.toFloat())
        drawIris(c, lensR, AnimMath.lerp(breath, 1f, opened), Art.ink, strokeW)
    }
}
