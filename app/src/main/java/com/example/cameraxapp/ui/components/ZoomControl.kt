package com.example.cameraxapp.ui.components

import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.ui.theme.InkColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalZeroShutterLag::class)
@Composable
fun ZoomControl(cameraControl: CameraControl, cameraInfo: CameraInfo) {
    val zoomState = cameraInfo.zoomState.observeAsState()
    val currentZoom = zoomState.value?.zoomRatio ?: 1f
    val maxZoom = zoomState.value?.maxZoomRatio ?: 1f
    val minZoom = zoomState.value?.minZoomRatio ?: 1f

    Slider(
        colors = SliderDefaults.colors(
            thumbColor = InkColors.textPrimary,
            activeTrackColor = InkColors.textPrimary,
            inactiveTrackColor = InkColors.textPrimary.copy(alpha = 0.35f),
        ),
        value = currentZoom,
        onValueChange = { zoomRatio ->
            cameraControl.setZoomRatio(zoomRatio)
        },
        valueRange = minZoom..maxZoom,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

/** Dark pill that shows the current zoom, such as "2x" or "1.5x". */
@Composable
fun ZoomPill(cameraInfo: CameraInfo, modifier: Modifier = Modifier) {
    val zoomState = cameraInfo.zoomState.observeAsState()
    val zoom = zoomState.value?.zoomRatio ?: 1f
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = zoomLabel(zoom),
            style = MaterialTheme.typography.labelMedium,
            color = InkColors.textPrimary,
        )
    }
}

private fun zoomLabel(ratio: Float): String =
    if (abs(ratio - ratio.roundToInt()) < 0.05f) {
        "${ratio.roundToInt()}x"
    } else {
        String.format(Locale.US, "%.1fx", ratio)
    }
