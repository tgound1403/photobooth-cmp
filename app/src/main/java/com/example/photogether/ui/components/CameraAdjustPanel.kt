package com.example.photogether.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photogether.core.camera.CameraAdjustState
import com.example.photogether.core.camera.CameraTool
import com.example.photogether.core.camera.EvMode
import com.example.photogether.core.camera.formatEv
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.AppIcons

/** Colour temperature (K) of the sun preset. */
const val SUN_PRESET_KELVIN = 5200

/** Colour temperature (K) of the bulb preset. */
const val BULB_PRESET_KELVIN = 3200

/**
 * Pill row over the bottom edge of the viewfinder: the white balance pill (thermometer, with the Kelvin value
 * when set), the [focalPill] slot, and the exposure pill (sun icon and EV value). A pill whose panel is open
 * becomes a cream chevron-down circle, and tapping it closes the panel. The callers decide which panel opens.
 */
@Composable
fun CameraQuickBar(
    kelvin: Int?,
    ev: Float,
    wbOpen: Boolean,
    evOpen: Boolean,
    onWbClick: () -> Unit,
    onEvClick: () -> Unit,
    modifier: Modifier = Modifier,
    focalPill: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        if (wbOpen) {
            CloseChevron(label = "Đóng cân bằng trắng", onClick = onWbClick)
        } else {
            CameraPill(
                text = kelvin?.toString(),
                icon = AppIcons.Temperature,
                selected = kelvin != null,
                label = "Cân bằng trắng",
                onClick = onWbClick,
            )
        }
        focalPill()
        if (evOpen) {
            CloseChevron(label = "Đóng phơi sáng", onClick = onEvClick)
        } else {
            CameraPill(
                text = formatEv(ev),
                icon = AppIcons.Sun,
                selected = false,
                label = "Phơi sáng",
                onClick = onEvClick,
            )
        }
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
 * Row beside the white balance ruler: a round "A" (automatic white balance), a round sun button (5200K) and a
 * round bulb button (3200K). The button for the current Kelvin value is cream; the others are dark.
 */
@Composable
fun CameraToolButtons(
    state: CameraAdjustState,
    onAuto: () -> Unit,
    onSun: () -> Unit,
    onBulb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AutoWbButton(selected = state.kelvin == null, onClick = onAuto)
        CameraCircleButton(
            icon = AppIcons.Sun,
            label = "Ánh sáng ban ngày 5200K",
            onClick = onSun,
            containerColor = toolFill(state.kelvin == SUN_PRESET_KELVIN),
            contentColor = toolContent(state.kelvin == SUN_PRESET_KELVIN),
        )
        CameraCircleButton(
            icon = AppIcons.Lightbulb,
            label = "Đèn sợi đốt 3200K",
            onClick = onBulb,
            containerColor = toolFill(state.kelvin == BULB_PRESET_KELVIN),
            contentColor = toolContent(state.kelvin == BULB_PRESET_KELVIN),
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

/** Cream circle with a chevron-down. Shown in place of an open pill; tapping it closes the panel. */
@Composable
private fun CloseChevron(label: String, onClick: () -> Unit) {
    CameraCircleButton(
        icon = AppIcons.ChevronDown,
        label = label,
        onClick = onClick,
        buttonSize = 40.dp,
        containerColor = InkColors.accent,
        contentColor = InkColors.onAccent,
    )
}

/** Dark translucent pill with an optional leading icon and optional text. Cream when [selected]. */
@Composable
private fun CameraPill(
    text: String?,
    icon: ImageVector?,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val content = toolContent(selected)
    Row(
        modifier = Modifier
            .semantics { contentDescription = label }
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
            if (!text.isNullOrEmpty()) Spacer(Modifier.width(Spacing.xs))
        }
        if (!text.isNullOrEmpty()) {
            Text(
                text = text,
                style = BinsooType.label,
                color = content,
            )
        }
    }
}

/** Round "A" button: automatic white balance. Cream while auto is in use. */
@Composable
private fun AutoWbButton(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(toolFill(selected))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Tự động cân bằng trắng" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "A",
            style = MaterialTheme.typography.titleMedium,
            color = toolContent(selected),
        )
    }
}

private val depthSheen = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.28f)),
)
private val depthRim = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.04f)),
)

/**
 * Gives a camera control depth on top of its fill: a light sheen from above, a darker base and a thin lit rim.
 * Place it right after the control's background, inside its clip.
 */
internal fun Modifier.depth(shape: Shape): Modifier =
    this.background(depthSheen, shape).border(1.dp, depthRim, shape)

/** Fill of a camera control: cream when [selected], otherwise dark translucent. */
internal fun toolFill(selected: Boolean): Color =
    if (selected) InkColors.accent else InkColors.surfaceElevated.copy(alpha = 0.7f)

/** Icon and text colour on a [toolFill]. */
internal fun toolContent(selected: Boolean): Color =
    if (selected) InkColors.onAccent else InkColors.textPrimary
