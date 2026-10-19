package com.example.photogether.ui.components

import androidx.annotation.OptIn
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Spacing
import kotlin.math.roundToInt
import com.example.photogether.ui.theme.AppIcons

/**
 * Focal pill on the viewfinder's bottom edge: the 35mm-equivalent focal length in use, such as "26". Tapping it
 * calls [onClick], which opens the lens panel. While [selected] (the panel is open) it becomes a cream
 * chevron-down circle that closes the panel.
 */
@OptIn(ExperimentalZeroShutterLag::class)
@Composable
fun FocalPill(
    cameraInfo: CameraInfo,
    lensMm: Float,
    cropZoom: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoomState = cameraInfo.zoomState.observeAsState()
    val zoom = zoomState.value?.zoomRatio ?: 1f
    val currentMm = (lensMm * zoom * cropZoom).roundToInt()
    if (selected) {
        CameraCircleButton(
            icon = AppIcons.ChevronDown,
            label = "Đóng tiêu cự",
            onClick = onClick,
            modifier = modifier,
            buttonSize = 40.dp,
            containerColor = InkColors.accent,
            contentColor = InkColors.onAccent,
        )
    } else {
        Column(
            modifier = modifier
                .semantics { contentDescription = "Tiêu cự ${currentMm}mm" }
                .clip(RoundedCornerShape(percent = 50))
                .background(toolFill(false))
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$currentMm",
                style = BinsooType.title,
                color = InkColors.textPrimary,
            )
        }
    }
}
