package com.example.cameraxapp.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.FilterCatalog
import com.example.cameraxapp.core.render.FilterInfo
import com.example.cameraxapp.ui.components.RulerSlider
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke

private val THUMB_SIZE = 64.dp

/** Filter strip with live thumbnails, plus the intensity ruler when a filter is applied. */
@Composable
fun FilterPanel(
    params: EditParams,
    filters: List<FilterInfo>,
    thumbnails: Map<String, Bitmap>,
    onRequestThumbnail: (String) -> Unit,
    onSelect: (String) -> Unit,
    onLive: (ParamsEdit) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(filters, key = { it.id }) { filter ->
                LaunchedEffect(filter.id) { onRequestThumbnail(filter.id) }
                FilterThumb(
                    filter = filter,
                    selected = params.filterId == filter.id,
                    thumbnail = thumbnails[filter.id],
                    onClick = { onSelect(filter.id) },
                )
            }
        }
        if (params.filterId != FilterCatalog.ORIGINAL_ID) {
            RulerSlider(
                label = "Intensity",
                value = params.filterIntensity,
                valueRange = 0f..1f,
                defaultValue = 1f,
                step = 0.05f,
                onValueChange = { v -> onLive { it.copy(filterIntensity = v) } },
                onValueChangeFinished = onCommit,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }
}

@Composable
private fun FilterThumb(
    filter: FilterInfo,
    selected: Boolean,
    thumbnail: Bitmap?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Column(
        modifier = Modifier.width(THUMB_SIZE).clickable(onClick = onClick),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(THUMB_SIZE)
                .clip(shape)
                .background(AppColors.surface)
                .border(
                    width = if (selected) Stroke.selected else Stroke.hairline,
                    color = if (selected) AppColors.accent else AppColors.outline,
                    shape = shape,
                ),
        ) {
            thumbnail?.let { bitmap ->
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = filter.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = filter.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AppColors.accent else AppColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
