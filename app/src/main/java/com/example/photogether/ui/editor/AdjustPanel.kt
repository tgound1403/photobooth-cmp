package com.example.photogether.ui.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.photogether.core.render.EditParams
import com.example.photogether.ui.theme.Spacing

// Most used first, so the chips people reach for are at the start of the row. Rarely used looks come last.
private val ADJUSTMENTS = listOf(
    ParamTool("Exposure", -1f..1f, { it.exposure }, { p, v -> p.copy(exposure = v) }),
    ParamTool("Contrast", -1f..1f, { it.contrast }, { p, v -> p.copy(contrast = v) }),
    ParamTool("Saturation", -1f..1f, { it.saturation }, { p, v -> p.copy(saturation = v) }),
    ParamTool("Temperature", -1f..1f, { it.temperature }, { p, v -> p.copy(temperature = v) }),
    ParamTool("Highlights", -1f..1f, { it.highlight }, { p, v -> p.copy(highlight = v) }),
    ParamTool("Shadows", -1f..1f, { it.shadow }, { p, v -> p.copy(shadow = v) }),
    ParamTool("Grain", 0f..1f, { it.grain }, { p, v -> p.copy(grain = v) }),
    ParamTool("Sharpness", 0f..1f, { it.sharpness }, { p, v -> p.copy(sharpness = v) }),
    ParamTool("Vignette", 0f..1f, { it.vignette }, { p, v -> p.copy(vignette = v) }),
    ParamTool("Fade", 0f..1f, { it.fade }, { p, v -> p.copy(fade = v) }),
    ParamTool("Tint", -1f..1f, { it.tint }, { p, v -> p.copy(tint = v) }),
    ParamTool("Chromatic", 0f..1f, { it.chromatic }, { p, v -> p.copy(chromatic = v) }),
    ParamTool("ND filter", 0f..1f, { it.nd }, { p, v -> p.copy(nd = v) }),
    ParamTool("Graduated ND", 0f..1f, { it.gnd }, { p, v -> p.copy(gnd = v) }),
)

/** Row of adjustment chips with the one chosen slider below; see [ParamToolStrip]. */
@Composable
fun AdjustPanel(
    params: EditParams,
    onLive: (ParamsEdit) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ParamToolStrip(
        tools = ADJUSTMENTS,
        params = params,
        selectionKey = "adjust",
        onLive = onLive,
        onCommit = onCommit,
        modifier = modifier.padding(vertical = Spacing.sm),
    )
}
