package com.example.photogether.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.photogether.core.camera.CAMERA_TINT_LIMIT
import com.example.photogether.core.render.Kelvin
import com.example.photogether.core.render.kelvinToSlider
import com.example.photogether.core.render.sliderToKelvin
import com.example.photogether.ui.theme.InkColors
import kotlin.math.roundToInt

private const val KELVIN_RULER_STEPS = 80
private const val KELVIN_MAJOR_TICK_EVERY = 10
/** One haptic tick per 0.05 of tint, which is 5 hundredths of the full range. */
private const val TINT_STEP = 0.05f

/** Fallback track. The camera pill draws the colour pad instead (see [RulerSlider]'s `colorPad`). */
private val KELVIN_TRACK: Brush = Brush.horizontalGradient(
    listOf(
        Color(0xFF4F8CFF),
        Color(0xFFD9E6FF),
        Color(0xFFF7F3EC),
        Color(0xFFFFE27A),
        Color(0xFFFFC21A),
    ),
)

/** Label shown for a white-balance value: "Auto" when null, otherwise "5500K". */
fun kelvinLabel(kelvin: Int?): String = if (kelvin == null) "Auto" else "${kelvin}K"

/** Readout for the tint: "Tint 0", "Tint +25" (magenta) or "Tint -25" (green), on a -100..100 scale. */
fun tintLabel(tint: Float): String {
    val n = (tint * 100f).roundToInt()
    return when {
        n > 0 -> "Tint +$n"
        n < 0 -> "Tint $n"
        else -> "Tint 0"
    }
}

/**
 * Colour-temperature ruler, drawn as a rainbow pill. The ruler works in slider position (0..1, linear
 * in mired), and every change is converted to a snapped Kelvin value before it leaves this
 * composable. The readout is shown by the camera screen, not by the ruler.
 *
 * Dragging up or down on the same ruler sets the tint, green to magenta. The tint readout sits above the
 * ruler, because the Kelvin pill is replaced by the close button while this panel is open.
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
    tint: Float = 0f,
    onTintChange: (Float) -> Unit = {},
    measuredKelvin: Int? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = tintLabel(tint),
            style = MaterialTheme.typography.labelSmall,
            color = InkColors.textSecondary,
        )
        RulerSlider(
            label = "WB",
            value = kelvinToSlider(kelvin ?: measuredKelvin ?: Kelvin.NEUTRAL),
            valueRange = 0f..1f,
            defaultValue = kelvinToSlider(Kelvin.NEUTRAL),
            step = 1f / KELVIN_RULER_STEPS,
            onValueChange = { pos ->
                val k = sliderToKelvin(pos)
                if (k != kelvin) onKelvinChange(k)
            },
            onValueChangeFinished = onChangeFinished,
            majorTickEvery = KELVIN_MAJOR_TICK_EVERY,
            track = KELVIN_TRACK,
            colorPad = true,
            showHeader = false,
            ink = true,
            verticalRange = -CAMERA_TINT_LIMIT..CAMERA_TINT_LIMIT,
            verticalValue = tint,
            verticalStep = TINT_STEP,
            onVerticalValueChange = onTintChange,
        )
    }
}
