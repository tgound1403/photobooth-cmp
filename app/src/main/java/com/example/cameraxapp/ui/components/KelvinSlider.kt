package com.example.cameraxapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.cameraxapp.core.render.Kelvin
import com.example.cameraxapp.core.render.kelvinToSlider
import com.example.cameraxapp.core.render.sliderToKelvin
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing

private const val KELVIN_RULER_STEPS = 80
private const val KELVIN_MAJOR_TICK_EVERY = 10

/** Label shown for a white-balance value: "Auto" when null, otherwise "5500K". */
fun kelvinLabel(kelvin: Int?): String = if (kelvin == null) "Auto" else "${kelvin}K"

/**
 * Colour-temperature ruler with an Auto chip. The ruler works in slider position (0..1, linear
 * in mired), and every change is converted to a snapped Kelvin value before it leaves this
 * composable. Auto is shown as the neutral position with the chip highlighted.
 */
@Composable
fun KelvinSlider(
    kelvin: Int?,
    onKelvinChange: (Int) -> Unit,
    onAuto: () -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RulerSlider(
            label = "WB",
            value = kelvinToSlider(kelvin ?: Kelvin.NEUTRAL),
            valueRange = 0f..1f,
            defaultValue = kelvinToSlider(Kelvin.NEUTRAL),
            step = 1f / KELVIN_RULER_STEPS,
            onValueChange = { pos ->
                val k = sliderToKelvin(pos)
                if (k != kelvin) onKelvinChange(k)
            },
            onValueChangeFinished = onChangeFinished,
            valueFormatter = { pos -> if (kelvin == null) "Auto" else kelvinLabel(sliderToKelvin(pos)) },
            majorTickEvery = KELVIN_MAJOR_TICK_EVERY,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.sm))
        AutoChip(selected = kelvin == null, onClick = onAuto)
    }
}

@Composable
private fun AutoChip(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(if (selected) AppColors.accent else AppColors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Auto",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AppColors.onAccent else AppColors.textPrimary,
        )
    }
}
