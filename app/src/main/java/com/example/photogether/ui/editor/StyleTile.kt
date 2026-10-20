package com.example.photogether.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke

private val TILE_WIDTH = 56.dp
private val TILE_HEIGHT = 64.dp

/**
 * Thumbnail tile with a short label under it: the style strip used by the Frame, Crop and
 * Overlay tabs. The selected tile gets the accent outline and accent label. [icon] draws the
 * tile's picture and receives the ink colour for the current state.
 */
@Composable
fun StyleTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.(ink: Color) -> Unit,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val ink = if (selected) AppColors.accent else AppColors.textPrimary
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(width = TILE_WIDTH, height = TILE_HEIGHT)
                .clip(shape)
                .background(AppColors.surface)
                .border(
                    if (selected) Stroke.selected else Stroke.hairline,
                    if (selected) AppColors.accent else AppColors.outline,
                    shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon(ink)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            maxLines = 1,
        )
    }
}
