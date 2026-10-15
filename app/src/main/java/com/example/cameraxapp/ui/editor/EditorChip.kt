package com.example.cameraxapp.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke

/** Pill-style option used by the editor panels. The selected chip gets the accent outline. */
@Composable
fun EditorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) AppColors.surfaceElevated else AppColors.surface)
            .border(
                width = if (selected) Stroke.selected else Stroke.hairline,
                color = if (selected) AppColors.accent else AppColors.outline,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AppColors.accent else AppColors.textPrimary,
            maxLines = 1,
        )
    }
}
