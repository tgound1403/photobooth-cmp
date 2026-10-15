package com.example.cameraxapp.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.FilterCatalog
import com.example.cameraxapp.core.render.FilterInfo
import com.example.cameraxapp.core.render.Recipe
import com.example.cameraxapp.ui.components.RulerSlider
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke
import com.tgound.cameraxapp.R

private val THUMB_SIZE = 64.dp

/** Two sections of the filter tab: catalog filters, and saved recipes. */
private enum class FilterSection(val label: String) { FILTERS("Filters"), RECIPES("Recipes") }

/**
 * Filter tab. The Filters section is the filter strip with live thumbnails and the intensity
 * ruler; the Recipes section lists saved recipes.
 */
@Composable
fun FilterPanel(
    params: EditParams,
    filters: List<FilterInfo>,
    thumbnails: Map<String, Bitmap>,
    onRequestThumbnail: (String) -> Unit,
    onSelect: (String) -> Unit,
    onLive: (ParamsEdit) -> Unit,
    onCommit: () -> Unit,
    recipes: List<Recipe>,
    canSaveRecipe: Boolean,
    onSaveRecipe: () -> Unit,
    onApplyRecipe: (EditParams) -> Unit,
    onImportRecipe: (String) -> Unit,
    /** Premium filters get a PRO badge until the user has Premium. */
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var section by rememberSaveable { mutableStateOf(FilterSection.FILTERS) }
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionRow(selected = section, onSelect = { section = it })
        when (section) {
            FilterSection.FILTERS -> FilterStripSection(params, filters, thumbnails, onRequestThumbnail, onSelect, onLive, onCommit, isPremium)
            FilterSection.RECIPES -> RecipePanel(
                params = params,
                recipes = recipes,
                canSave = canSaveRecipe,
                onSave = onSaveRecipe,
                onApply = onApplyRecipe,
                onImport = onImportRecipe,
            )
        }
    }
}

@Composable
private fun SectionRow(selected: FilterSection, onSelect: (FilterSection) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        FilterSection.values().forEach { section ->
            Text(
                text = section.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (section == selected) AppColors.accent else AppColors.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable { onSelect(section) }
                    .padding(vertical = Spacing.xs),
            )
        }
    }
}

@Composable
private fun FilterStripSection(
    params: EditParams,
    filters: List<FilterInfo>,
    thumbnails: Map<String, Bitmap>,
    onRequestThumbnail: (String) -> Unit,
    onSelect: (String) -> Unit,
    onLive: (ParamsEdit) -> Unit,
    onCommit: () -> Unit,
    isPremium: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
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
                    showPro = filter.premium && !isPremium,
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
    showPro: Boolean,
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
            if (showPro) {
                Text(
                    text = stringResource(R.string.pro_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.onAccent,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(Spacing.xs)
                        .background(AppColors.accent, RoundedCornerShape(Radius.sm))
                        .padding(horizontal = Spacing.xs),
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
