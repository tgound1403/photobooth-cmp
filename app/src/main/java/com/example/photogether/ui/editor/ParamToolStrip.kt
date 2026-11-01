package com.example.photogether.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.photogether.core.render.EditParams
import com.example.photogether.ui.components.RulerSlider
import com.example.photogether.ui.components.defaultRulerFormat
import com.example.photogether.ui.theme.Spacing

/** One adjustable value of [EditParams]: how to read and write it, its range and its neutral value. */
class ParamTool(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val read: (EditParams) -> Float,
    val write: (EditParams, Float) -> EditParams,
    val default: Float = 0f,
    val step: Float = 0.05f,
)

/**
 * A horizontal row of tool chips with a single slider under it: only the chosen tool's slider is on screen,
 * so nothing needs scrolling. A chip shows its readout after the name when its value is not the neutral one
 * (for example "Exposure +20"); a change too small to show as a whole number gets a dot instead.
 * The selection survives rotation and tab changes through [selectionKey].
 */
@Composable
fun ParamToolStrip(
    tools: List<ParamTool>,
    params: EditParams,
    selectionKey: String,
    onLive: (ParamsEdit) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    sidePadding: Dp = Spacing.lg,
) {
    var selected by rememberSaveable(selectionKey) { mutableStateOf(0) }
    val tool = tools[selected.coerceIn(0, tools.lastIndex)]

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            tools.forEachIndexed { i, t ->
                val value = t.read(params)
                val changed = value != t.default
                val readout = defaultRulerFormat(value, t.range)
                EditorChip(
                    label = when {
                        !changed -> t.label
                        readout == "0" -> "${t.label} •"
                        else -> "${t.label} $readout"
                    },
                    selected = i == selected,
                    onClick = { selected = i },
                )
            }
        }
        RulerSlider(
            label = tool.label,
            value = tool.read(params),
            valueRange = tool.range,
            defaultValue = tool.default,
            step = tool.step,
            onValueChange = { v -> onLive { tool.write(it, v) } },
            onValueChangeFinished = onCommit,
            modifier = Modifier.padding(horizontal = sidePadding),
        )
    }
}
