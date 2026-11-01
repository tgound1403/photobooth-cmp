package com.example.photogether.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.photogether.core.moments.CalendarColors
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.AppIcons
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.tgound.photogether.R

private val SWATCH = 44.dp

private val RAINBOW = listOf(
    Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFE53935),
)

/** Small round dot showing the current calendar colour, for the settings row. */
@Composable
fun CalendarColorDot(color: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(Color(color), CircleShape)
            .border(1.dp, AppColors.outline.copy(alpha = 0.4f), CircleShape),
    )
}

/**
 * Bottom sheet for the calendar colour: the app's suggestions plus a custom swatch that opens
 * red/green/blue sliders and a hex field, so any colour can be used.
 * A suggestion applies at once and closes the sheet; a custom colour applies with its button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarColorSheet(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val names = stringArrayResource(R.array.calendar_color_names)
    val isCustom = CalendarColors.SUGGESTED.none { it == selected }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.settings_calendar_color),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary,
            )
            Text(
                text = stringResource(R.string.settings_calendar_color_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.xs))

            // Suggestions, then the custom swatch last; four to a row.
            val cells = CalendarColors.SUGGESTED.size + 1
            for (start in 0 until cells step 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (i in start until minOf(start + 4, cells)) {
                        if (i < CalendarColors.SUGGESTED.size) {
                            val color = CalendarColors.SUGGESTED[i]
                            Swatch(
                                fill = Brush.linearGradient(listOf(Color(color), Color(color))),
                                tone = color,
                                selected = color == selected && !picking,
                                label = names.getOrElse(i) { CalendarColors.hex(color) },
                                onClick = { onSelect(color); onDismiss() },
                            )
                        } else {
                            Swatch(
                                fill = Brush.sweepGradient(RAINBOW),
                                tone = if (isCustom) selected else Color.Transparent.toArgb(),
                                selected = isCustom || picking,
                                label = stringResource(R.string.settings_calendar_custom),
                                onClick = { picking = !picking },
                                centre = isCustom,
                            )
                        }
                    }
                    // Keep a short last row aligned with the full rows above it.
                    repeat(start + 4 - cells.coerceAtMost(start + 4)) { Spacer(Modifier.size(width = 72.dp, height = 1.dp)) }
                }
            }

            if (picking) {
                CustomColorPanel(
                    initial = selected,
                    onApply = { onSelect(it); onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun Swatch(
    fill: Brush,
    tone: Int,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    centre: Boolean = false,
) {
    val ring = if (selected) AppColors.accent else AppColors.outline.copy(alpha = 0.3f)
    // A light swatch needs a dark tick and the other way round.
    val tick = if (CalendarColors.isLight(tone)) Color(0xFF0B0B0B) else Color.White
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 72.dp, height = 70.dp)) {
        Box(
            modifier = Modifier
                .size(SWATCH)
                .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
                .padding(if (selected) 4.dp else 0.dp)
                .background(fill, CircleShape)
                .clickable(onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            if (centre) {
                Box(Modifier.size(SWATCH / 2).background(Color(tone), CircleShape))
            } else if (selected) {
                Icon(AppIcons.Check, contentDescription = null, tint = tick, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.textSecondary,
            maxLines = 1,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

/** Red, green and blue sliders with a live preview, a hex field and an apply button. */
@Composable
private fun CustomColorPanel(initial: Int, onApply: (Int) -> Unit) {
    var red by remember { mutableIntStateOf((initial shr 16) and 0xFF) }
    var green by remember { mutableIntStateOf((initial shr 8) and 0xFF) }
    var blue by remember { mutableIntStateOf(initial and 0xFF) }
    val current = CalendarColors.opaque((red shl 16) or (green shl 8) or blue)
    // What the user is typing; kept apart so a half-typed hex does not jump around.
    var hexText by remember { mutableStateOf(CalendarColors.hex(current)) }

    fun channelChanged() {
        hexText = CalendarColors.hex(CalendarColors.opaque((red shl 16) or (green shl 8) or blue))
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(current), RoundedCornerShape(Radius.md))
                .border(1.dp, AppColors.outline.copy(alpha = 0.35f), RoundedCornerShape(Radius.md)),
        )
        ChannelSlider("R", red) { red = it; channelChanged() }
        ChannelSlider("G", green) { green = it; channelChanged() }
        ChannelSlider("B", blue) { blue = it; channelChanged() }
        OutlinedTextField(
            value = hexText,
            onValueChange = { text ->
                hexText = text
                CalendarColors.parseHex(text)?.let {
                    red = (it shr 16) and 0xFF
                    green = (it shr 8) and 0xFF
                    blue = it and 0xFF
                }
            },
            singleLine = true,
            isError = CalendarColors.parseHex(hexText) == null,
            label = { Text("HEX") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(AppColors.accent, RoundedCornerShape(Radius.sm))
                .clickable { onApply(current) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_calendar_apply),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.onAccent,
            )
        }
    }
}

@Composable
private fun ChannelSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textPrimary,
            modifier = Modifier.padding(end = Spacing.sm),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}
