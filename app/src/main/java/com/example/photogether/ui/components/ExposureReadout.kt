package com.example.photogether.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Values shown by [ExposureReadout]. [ev] is null in manual mode, where exposure compensation does not apply. */
data class ExposureReadoutValues(val tv: String, val iso: String, val ev: String?)

/**
 * Exposure values drawn over the live view in the monospace style of a camera's viewfinder: shutter (TV),
 * ISO and, in auto, exposure compensation (EV). Sits on a small dark plate so it reads over any preview.
 */
@Composable
fun ExposureReadout(values: ExposureReadoutValues, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        ReadoutItem("TV ${values.tv}")
        ReadoutItem("ISO ${values.iso}")
        values.ev?.let { ReadoutItem("EV $it") }
    }
}

@Composable
private fun ReadoutItem(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 6.dp),
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White,
        ),
        maxLines = 1,
    )
}
