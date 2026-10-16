package com.example.photogether.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.photogether.core.render.Kelvin
import com.example.photogether.core.render.kelvinToSlider
import com.example.photogether.core.render.sliderToKelvin

private const val KELVIN_RULER_STEPS = 80
private const val KELVIN_MAJOR_TICK_EVERY = 10

/** Warm orange at the low end of the slider, neutral in the middle, cool blue at the high end. */
private val KELVIN_TRACK: Brush = Brush.horizontalGradient(
    listOf(
        Color(0xFFFF9A3D),
        Color(0xFFFFD2A0),
        Color(0xFFF7F3EC),
        Color(0xFFBFD9FF),
        Color(0xFF6E9EFF),
    ),
)

/** Label shown for a white-balance value: "Auto" when null, otherwise "5500K". */
fun kelvinLabel(kelvin: Int?): String = if (kelvin == null) "Auto" else "${kelvin}K"

/**
 * Colour-temperature ruler, drawn as a rainbow pill. The ruler works in slider position (0..1, linear
 * in mired), and every change is converted to a snapped Kelvin value before it leaves this
 * composable. The readout is shown by the camera screen, not by the ruler.
 *
 * [onAuto] is no longer drawn here. The auto button is the "A" beside the ruler (see
 * [CameraToolButtons]). The parameter stays so existing callers still compile.
 */
@Composable
fun KelvinSlider(
    kelvin: Int?,
    onKelvinChange: (Int) -> Unit,
    onAuto: () -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        majorTickEvery = KELVIN_MAJOR_TICK_EVERY,
        modifier = modifier,
        track = KELVIN_TRACK,
        showHeader = false,
        ink = true,
    )
}
