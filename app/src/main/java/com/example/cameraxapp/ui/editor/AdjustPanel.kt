package com.example.cameraxapp.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.ui.components.RulerSlider
import com.example.cameraxapp.ui.theme.Spacing

private const val ADJUST_STEP = 0.05f

/** One global adjustment: how to read it, write it and which range it covers. Default is 0. */
private class Adjustment(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val read: (EditParams) -> Float,
    val write: (EditParams, Float) -> EditParams,
)

private val ADJUSTMENTS = listOf(
    Adjustment("Exposure", -1f..1f, { it.exposure }, { p, v -> p.copy(exposure = v) }),
    Adjustment("Contrast", -1f..1f, { it.contrast }, { p, v -> p.copy(contrast = v) }),
    Adjustment("Saturation", -1f..1f, { it.saturation }, { p, v -> p.copy(saturation = v) }),
    Adjustment("Temperature", -1f..1f, { it.temperature }, { p, v -> p.copy(temperature = v) }),
    Adjustment("Tint", -1f..1f, { it.tint }, { p, v -> p.copy(tint = v) }),
    Adjustment("Highlights", -1f..1f, { it.highlight }, { p, v -> p.copy(highlight = v) }),
    Adjustment("Shadows", -1f..1f, { it.shadow }, { p, v -> p.copy(shadow = v) }),
    Adjustment("Fade", 0f..1f, { it.fade }, { p, v -> p.copy(fade = v) }),
    Adjustment("Sharpness", 0f..1f, { it.sharpness }, { p, v -> p.copy(sharpness = v) }),
    Adjustment("Grain", 0f..1f, { it.grain }, { p, v -> p.copy(grain = v) }),
    Adjustment("Vignette", 0f..1f, { it.vignette }, { p, v -> p.copy(vignette = v) }),
    Adjustment("Chromatic", 0f..1f, { it.chromatic }, { p, v -> p.copy(chromatic = v) }),
)

/** Scrollable list of rulers for the global adjustments. */
@Composable
fun AdjustPanel(
    params: EditParams,
    onLive: (ParamsEdit) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ADJUSTMENTS.forEach { adjustment ->
            RulerSlider(
                label = adjustment.label,
                value = adjustment.read(params),
                valueRange = adjustment.range,
                defaultValue = 0f,
                step = ADJUST_STEP,
                onValueChange = { v -> onLive { adjustment.write(it, v) } },
                onValueChangeFinished = onCommit,
            )
        }
    }
}
