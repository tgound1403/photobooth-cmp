package com.example.cameraxapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.core.camera.CameraAdjustState
import com.example.cameraxapp.core.camera.CameraTool
import com.example.cameraxapp.core.camera.EvMode
import com.example.cameraxapp.core.camera.formatEv
import com.example.cameraxapp.ui.theme.InkColors
import com.example.cameraxapp.ui.theme.Spacing

/**
 * Pill row over the bottom edge of the viewfinder: a white chevron that collapses the open panel,
 * the optional [zoomPill] slot (CameraScreen passes [ZoomPill]), and the EV pill (sun icon and value).
 * Tapping the EV pill toggles the exposure panel, the same as the sun button below the viewfinder.
 */
@Composable
fun CameraQuickBar(
    state: CameraAdjustState,
    onToolClick: (CameraTool) -> Unit,
    modifier: Modifier = Modifier,
    zoomPill: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        CameraCircleButton(
            icon = Icons.Default.KeyboardArrowDown,
            label = "Thu gọn bảng điều chỉnh",
            onClick = { state.openTool?.let(onToolClick) },
            buttonSize = 40.dp,
            containerColor = InkColors.textPrimary,
            contentColor = InkColors.onAccent,
        )
        zoomPill()
        CameraPill(
            text = formatEv(state.ev),
            icon = Icons.Default.WbSunny,
            selected = state.openTool == CameraTool.EXPOSURE,
            onClick = { onToolClick(CameraTool.EXPOSURE) },
        )
    }
}

/**
 * Ruler for the tool in [CameraAdjustState.openTool]. Hidden when no tool is open. The last open
 * tool stays in composition during the exit animation, so the ruler does not go blank as it closes.
 *
 * EV is disabled only when hardware EV is in use and manual exposure has turned AE off, because
 * the camera then ignores exposure compensation. Software EV still works in that case.
 */
@Composable
fun CameraAdjustPanel(
    state: CameraAdjustState,
    onKelvinChange: (Int) -> Unit,
    onKelvinAuto: () -> Unit,
    onEvChange: (Float) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastTool by remember { mutableStateOf(state.openTool) }
    if (state.openTool != null) lastTool = state.openTool

    AnimatedVisibility(
        visible = state.openTool != null,
        modifier = modifier,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
    ) {
        when (lastTool) {
            CameraTool.WHITE_BALANCE -> KelvinSlider(
                kelvin = state.kelvin,
                onKelvinChange = onKelvinChange,
                onAuto = onKelvinAuto,
                onChangeFinished = onCommit,
            )
            CameraTool.EXPOSURE -> EvSlider(
                ev = state.ev,
                range = state.evRange,
                enabled = !(state.manualExposureActive && state.evMode == EvMode.HARDWARE),
                onEvChange = onEvChange,
                onChangeFinished = onCommit,
            )
            null -> Unit
        }
    }
}

/**
 * Row beside the ruler: a cream "A" (automatic white balance), then a round dark sun button (exposure
 * panel) and a round dark bulb button (white balance panel). The open tool's button turns cream.
 */
@Composable
fun CameraToolButtons(
    state: CameraAdjustState,
    onAuto: () -> Unit,
    onToolClick: (CameraTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AutoWbButton(onClick = onAuto)
        CameraCircleButton(
            icon = Icons.Default.WbSunny,
            label = "Phơi sáng",
            onClick = { onToolClick(CameraTool.EXPOSURE) },
            containerColor = toolFill(state.openTool == CameraTool.EXPOSURE),
            contentColor = toolContent(state.openTool == CameraTool.EXPOSURE),
        )
        CameraCircleButton(
            icon = Icons.Default.Lightbulb,
            label = "Cân bằng trắng",
            onClick = { onToolClick(CameraTool.WHITE_BALANCE) },
            containerColor = toolFill(state.openTool == CameraTool.WHITE_BALANCE),
            contentColor = toolContent(state.openTool == CameraTool.WHITE_BALANCE),
        )
    }
}

/** Round icon button for the camera screen. Dark translucent by default, like the other camera chrome. */
@Composable
fun CameraCircleButton(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 48.dp,
    containerColor: Color = InkColors.surfaceElevated.copy(alpha = 0.7f),
    contentColor: Color = InkColors.textPrimary,
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics { if (label != null) contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Dark translucent pill with an optional leading icon. Cream when [selected]. */
@Composable
private fun CameraPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    val content = toolContent(selected)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(toolFill(selected))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

/** The cream "A" button: automatic white balance. */
@Composable
private fun AutoWbButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(InkColors.accent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Tự động cân bằng trắng" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "A",
            style = MaterialTheme.typography.titleMedium,
            color = InkColors.onAccent,
        )
    }
}

private fun toolFill(selected: Boolean): Color =
    if (selected) InkColors.accent else InkColors.surfaceElevated.copy(alpha = 0.7f)

private fun toolContent(selected: Boolean): Color =
    if (selected) InkColors.onAccent else InkColors.textPrimary
