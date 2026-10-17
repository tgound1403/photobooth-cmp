package com.example.photogether.ui.components

import androidx.annotation.OptIn
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.photogether.core.camera.FOCAL_STOPS_MM
import com.example.photogether.ui.theme.InkColors
import kotlin.math.roundToInt

/**
 * Zoom slider. The shown zoom is the camera ratio times [cropZoom]; changes go to [onZoom] (the view model),
 * which decides whether the camera or the crop frame takes them.
 */
@OptIn(ExperimentalZeroShutterLag::class)
@Composable
fun ZoomControl(cameraInfo: CameraInfo, cropZoom: Float, onZoom: (Float) -> Unit) {
    val zoomState = cameraInfo.zoomState.observeAsState()
    val currentZoom = (zoomState.value?.zoomRatio ?: 1f) * cropZoom
    val maxZoom = zoomState.value?.maxZoomRatio ?: 1f
    val minZoom = zoomState.value?.minZoomRatio ?: 1f

    Slider(
        colors = SliderDefaults.colors(
            thumbColor = InkColors.textPrimary,
            activeTrackColor = InkColors.textPrimary,
            inactiveTrackColor = InkColors.textPrimary.copy(alpha = 0.35f),
        ),
        value = currentZoom,
        onValueChange = onZoom,
        valueRange = minZoom..maxZoom,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

/**
 * Dark pill that shows the simulated 35mm-equivalent focal length, such as "35mm" or "75mm". [lensMm]
 * is the focal length of the lens in use and the shown value is lensMm times the zoom ratio and [cropZoom]. Tapping
 * calls [onTap] with the next focal stop in [FOCAL_STOPS_MM] that is above the current one, wrapping
 * back to the first stop.
 */
@OptIn(ExperimentalZeroShutterLag::class)
@Composable
fun ZoomPill(
    cameraInfo: CameraInfo,
    lensMm: Float,
    cropZoom: Float,
    onTap: (targetMm: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoomState = cameraInfo.zoomState.observeAsState()
    val zoom = zoomState.value?.zoomRatio ?: 1f
    val maxZoom = zoomState.value?.maxZoomRatio ?: 1f
    val currentMm = (lensMm * zoom * cropZoom).roundToInt()
    // Stops the lens can reach by digital zoom alone. Telephoto lenses are reached by switching lens.
    val reachableMm = lensMm * maxZoom
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
            .clickable {
                val next = FOCAL_STOPS_MM.firstOrNull { mm ->
                    mm > currentMm + 1 && mm <= reachableMm + 0.5f
                } ?: FOCAL_STOPS_MM.first()
                onTap(next)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${currentMm}mm",
            style = MaterialTheme.typography.labelMedium,
            color = InkColors.textPrimary,
        )
    }
}
