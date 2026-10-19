package com.example.photogether.ui.view

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.core.render.FilterInfo
import com.example.photogether.ui.components.DefaultAppBar
import com.example.photogether.ui.components.FilterStrip
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Motion
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel

@Composable
fun PhotoBoothSelectionScreen(
    navController: NavController,
    viewModel: PhotoBoothViewModel,
) {
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val capturedImages by viewModel.capturedImages.collectAsStateWithLifecycle()
    val requiredCount by viewModel.requiredPhotoCount.collectAsStateWithLifecycle()
    val filterSources by viewModel.filterSources.collectAsStateWithLifecycle()
    val selectedSourceId by viewModel.selectedSourceId.collectAsStateWithLifecycle()
    val thumbnails by viewModel.filteredThumbs.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    // Leaving this screen drops every shot of the session, so confirm first, for the system back too.
    BackHandler(enabled = capturedImages.isNotEmpty()) { showDiscardDialog = true }

    // Re-render the grid thumbnails whenever the look or the shots change.
    LaunchedEffect(capturedImages, selectedSourceId) {
        viewModel.refreshFilteredThumbs()
    }

    PhotoBoothSelectionContent(
        capturedImages = capturedImages,
        thumbnails = thumbnails,
        selectedImages = selectedImages,
        requiredCount = requiredCount,
        filters = filterSources,
        selectedFilterId = selectedSourceId,
        onFilterSelect = { viewModel.selectFilterSource(it) },
        onBack = {
            if (capturedImages.isEmpty()) navController.navigateUp() else showDiscardDialog = true
        },
        onToggle = { viewModel.toggleImageSelection(it) },
        onConfirm = {
            val images = selectedImages.toList()
            if (images.size == requiredCount) {
                // Go to the edit screen with the chosen images.
                navController.navigate(AppRoutes.photoBoothEdit(images))
            }
        }
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.booth_discard_title)) },
            text = { Text(stringResource(R.string.booth_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.clearCapturedImages()
                    navController.navigateUp()
                }) {
                    Text(stringResource(R.string.booth_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.booth_discard_keep))
                }
            },
        )
    }

    PhotoBoothPaywallOverlay(viewModel)
}

/** Stateless selection body. */
@Composable
fun PhotoBoothSelectionContent(
    capturedImages: List<String>,
    /** Shot path -> copy rendered with the chosen look. Shots not in the map show the original. */
    thumbnails: Map<String, Bitmap> = emptyMap(),
    selectedImages: Collection<String>,
    requiredCount: Int,
    filters: List<FilterInfo>,
    selectedFilterId: String,
    onFilterSelect: (String) -> Unit,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    // Fade the grid in on first composition.
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(Motion.DurationLong))
    }

    // Order of picks is the order of the strip, so each thumbnail shows its slot number.
    val pickOrder = remember(selectedImages) { selectedImages.toList() }

    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = stringResource(R.string.booth_selection_title)) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(Spacing.lg)
            ) {
                GlassButton(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.booth_edit),
                    enabled = selectedImages.size == requiredCount
                )
            }
        },
        containerColor = AppColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer { alpha = alphaAnim.value }
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            FilterStrip(
                filters = filters,
                selectedId = selectedFilterId,
                onSelect = onFilterSelect,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.booth_selection_counter, selectedImages.size, requiredCount),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.textPrimary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = Spacing.lg)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.weight(1f)
            ) {
                items(capturedImages) { imagePath ->
                    val isSelected = selectedImages.contains(imagePath)
                    val slot = pickOrder.indexOf(imagePath) + 1
                    Box(
                        modifier = Modifier
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(Radius.sm))
                            .border(
                                width = if (isSelected) Stroke.selected else Stroke.hairline,
                                color = if (isSelected) AppColors.accent else AppColors.outline,
                                shape = RoundedCornerShape(Radius.sm)
                            )
                            .clickable { onToggle(imagePath) }
                    ) {
                        val filtered = thumbnails[imagePath]
                        if (filtered != null) {
                            Image(
                                bitmap = filtered.asImageBitmap(),
                                contentDescription = stringResource(R.string.booth_captured_image),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = stringResource(R.string.booth_captured_image),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppColors.accent.copy(alpha = 0.3f))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(Spacing.xs)
                                    .size(24.dp)
                                    .background(AppColors.accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slot.toString(),
                                    color = AppColors.onAccent,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoBoothSelectionPreview() {
    PhotogetherTheme {
        PhotoBoothSelectionContent(
            capturedImages = emptyList(),
            selectedImages = emptySet(),
            requiredCount = 2,
            filters = emptyList(),
            selectedFilterId = "original",
            onFilterSelect = {},
            onBack = {},
            onToggle = {},
            onConfirm = {}
        )
    }
}
