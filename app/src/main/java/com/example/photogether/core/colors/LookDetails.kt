package com.example.photogether.core.colors

import com.example.photogether.core.render.EditParams
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Describes a look in Vietnamese, one line per value that moves the photo away from its plain
 * filter. Pure Kotlin, usable from JVM unit tests.
 */
object LookDetails {

    fun lines(params: EditParams): List<String> = buildList {
        if (params.filterIntensity < 1f) add("Cường độ bộ lọc: ${(params.filterIntensity * 100).roundToInt()}%")
        if (params.exposure != 0f) add("Phơi sáng: ${signedEv(params.exposure * 2)} EV")
        if (params.saturation != 0f) add("Độ bão hòa: ${signedPercent(params.saturation)}")
        if (params.contrast != 0f) add("Tương phản: ${signedPercent(params.contrast)}")
        if (params.highlight != 0f) add("Vùng sáng: ${signedPercent(params.highlight)}")
        if (params.shadow != 0f) add("Vùng tối: ${signedPercent(params.shadow)}")
    }

    private fun signedPercent(value: Float): String =
        String.format(Locale.US, "%+d%%", (value * 100).roundToInt())

    private fun signedEv(value: Float): String = String.format(Locale.US, "%+.2f", value)
}
