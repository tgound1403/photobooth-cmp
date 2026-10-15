package com.example.cameraxapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.cameraxapp.core.render.FilterInfo
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke

/** Horizontal strip of filter chips. The selected chip gets the accent outline. */
@Composable
fun FilterStrip(
    filters: List<FilterInfo>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(filters, key = { it.id }) { filter ->
            FilterChip(
                name = filter.name,
                selected = filter.id == selectedId,
                onClick = { onSelect(filter.id) },
            )
        }
    }
}

@Composable
private fun FilterChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = Modifier
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
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AppColors.textPrimary else AppColors.textSecondary,
        )
    }
}
