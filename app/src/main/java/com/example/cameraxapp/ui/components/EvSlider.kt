package com.example.cameraxapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cameraxapp.core.camera.EvRange
import com.example.cameraxapp.core.camera.formatEv

/**
 * Exposure-compensation ruler. Values snap to the steps of [range], and the readout uses
 * [formatEv]. Pass [enabled] = false when the camera ignores EV, such as with manual exposure.
 */
@Composable
fun EvSlider(
    ev: Float,
    range: EvRange,
    enabled: Boolean,
    onEvChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RulerSlider(
        label = "EV",
        value = ev,
        valueRange = range.minEv..range.maxEv,
        defaultValue = 0f,
        step = range.step,
        onValueChange = { raw ->
            val snapped = range.evFor(range.indexFor(raw))
            if (snapped != ev) onEvChange(snapped)
        },
        onValueChangeFinished = onChangeFinished,
        valueFormatter = { raw -> formatEv(range.evFor(range.indexFor(raw))) },
        majorTickEvery = range.majorTickEvery,
        enabled = enabled,
        modifier = modifier,
    )
}
