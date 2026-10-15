package com.example.cameraxapp.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.Recipe
import com.example.cameraxapp.core.render.RecipeCode
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke
import java.io.File

private val RECIPE_THUMB_SIZE = 64.dp

/**
 * Recipes tab of the filter panel: save the current edit, tap a recipe to apply it, copy a
 * recipe's share code, or paste a code to import it. Applying is one undoable step.
 */
@Composable
fun RecipePanel(
    params: EditParams,
    recipes: List<Recipe>,
    canSave: Boolean,
    onSave: () -> Unit,
    onApply: (EditParams) -> Unit,
    onImport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassButton(onClick = onSave, text = "Lưu recipe", enabled = canSave)
        }

        if (recipes.isEmpty()) {
            Text(
                text = "No recipes yet. Adjust a photo and save it as a recipe.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipeItem(
                        recipe = recipe,
                        selected = recipe.params == params,
                        onApply = { onApply(recipe.params) },
                        onCopy = { clipboard.setText(AnnotatedString(RecipeCode.encode(recipe.params))) },
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                singleLine = true,
                placeholder = { Text("PGR1:…", color = AppColors.textSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.textPrimary,
                    unfocusedTextColor = AppColors.textPrimary,
                    focusedBorderColor = AppColors.accent,
                    unfocusedBorderColor = AppColors.outline,
                ),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { clipboard.getText()?.text?.let { code = it } }) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = "Paste recipe code",
                    tint = AppColors.textSecondary,
                )
            }
            TextButton(
                onClick = {
                    onImport(code)
                    code = ""
                },
                enabled = code.isNotBlank(),
            ) {
                Text("Import")
            }
        }
    }
}

@Composable
private fun RecipeItem(
    recipe: Recipe,
    selected: Boolean,
    onApply: () -> Unit,
    onCopy: () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Column(
        modifier = Modifier.width(RECIPE_THUMB_SIZE).clickable(onClick = onApply),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(RECIPE_THUMB_SIZE)
                .clip(shape)
                .background(AppColors.surface)
                .border(
                    width = if (selected) Stroke.selected else Stroke.hairline,
                    color = if (selected) AppColors.accent else AppColors.outline,
                    shape = shape,
                ),
        ) {
            recipe.thumbnailPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = recipe.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = recipe.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AppColors.accent else AppColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        IconButton(onClick = onCopy, modifier = Modifier.size(IconSize.xl)) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy recipe code",
                tint = AppColors.textSecondary,
                modifier = Modifier.size(IconSize.sm),
            )
        }
    }
}
