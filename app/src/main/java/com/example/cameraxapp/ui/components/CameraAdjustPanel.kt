package com.example.cameraxapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.cameraxapp.core.camera.CameraAdjustState
import com.example.cameraxapp.core.camera.CameraTool
import com.example.cameraxapp.core.camera.EvMode
import com.example.cameraxapp.core.camera.formatEv
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing

/**
 * Two chips above the viewfinder: white balance and exposure. Each shows the current value.
 * Tapping a chip toggles its panel in [CameraAdjustPanel].
 */
@Composable
fun CameraQuickBar(
    state: CameraAdjustState,
    onToolClick: (CameraTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        QuickChip(
            text = "WB ${kelvinLabel(state.kelvin)}",
            selected = state.openTool == CameraTool.WHITE_BALANCE,
            onClick = { onToolClick(CameraTool.WHITE_BALANCE) },
        )
        QuickChip(
            text = "EV ${formatEv(state.ev)}",
            selected = state.openTool == CameraTool.EXPOSURE,
            onClick = { onToolClick(CameraTool.EXPOSURE) },
        )
    }
}

/**
 * Panel for the tool in [CameraAdjustState.openTool]. Hidden when no tool is open. The last open
 * tool stays in composition during the exit animation, so the panel does not go blank as it closes.
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
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        GlassBox(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            cornerRadius = Radius.md,
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
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
    }
}

@Composable
private fun QuickChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(if (selected) AppColors.accent else AppColors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AppColors.onAccent else AppColors.textPrimary,
        )
    }
}
