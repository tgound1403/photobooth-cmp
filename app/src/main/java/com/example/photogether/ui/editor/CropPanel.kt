package com.example.photogether.ui.editor

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.photogether.core.render.CropAspect
import com.example.photogether.core.render.CropParams
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Spacing

/** Aspect presets, quarter-turn rotation and flips. Each tap is a discrete, undoable change. */
@Composable
fun CropPanel(
    crop: CropParams,
    onCrop: (CropParams) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SectionLabel("Aspect")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CropAspect.values().forEach { aspect ->
                EditorChip(
                    label = aspect.label(),
                    selected = crop.aspect == aspect,
                    onClick = { onCrop(crop.copy(aspect = aspect)) },
                )
            }
        }
        SectionLabel("Transform")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            EditorChip(
                label = stringResource(R.string.crop_rotate),
                selected = false,
                onClick = { onCrop(crop.copy(quarterTurns = (crop.quarterTurns + 1) % 4)) },
            )
            EditorChip(
                label = stringResource(R.string.crop_flip_h),
                selected = crop.flipHorizontal,
                onClick = { onCrop(crop.copy(flipHorizontal = !crop.flipHorizontal)) },
            )
            EditorChip(
                label = stringResource(R.string.crop_flip_v),
                selected = crop.flipVertical,
                onClick = { onCrop(crop.copy(flipVertical = !crop.flipVertical)) },
            )
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = AppColors.textSecondary)
}

private fun CropAspect.label(): String =
    when (this) {
        CropAspect.FREE -> "Free"
        CropAspect.SQUARE -> "1:1"
        CropAspect.R4_5 -> "4:5"
        CropAspect.R3_4 -> "3:4"
        CropAspect.R9_16 -> "9:16"
    }
