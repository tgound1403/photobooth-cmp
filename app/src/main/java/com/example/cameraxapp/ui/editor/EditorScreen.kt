package com.example.cameraxapp.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cameraxapp.core.extension.findActivity
import com.example.cameraxapp.data.billing.BillingRepository
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.view.PaywallScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import org.koin.androidx.compose.get
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/** Bottom tabs of the editor. */
enum class EditorTab(val label: String, val icon: ImageVector) {
    FILTER("Filter", Icons.Default.FilterVintage),
    ADJUST("Adjust", Icons.Default.Tune),
    CROP("Crop", Icons.Default.Crop),
    FRAME("Frame", Icons.Default.Collections),
    OVERLAY("Overlay", Icons.Default.AutoAwesome),
}

/**
 * Photo editor. Hold the preview to see the original. Undo and redo step through committed
 * changes. Save writes a new JPEG and shows a confirmation.
 */
@Composable
fun EditorScreen(
    imagePath: String,
    navController: NavController,
    viewModel: EditorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val billing: BillingRepository = get()
    val prices by billing.prices.collectAsState()
    val activity = LocalContext.current.findActivity()
    val density = LocalDensity.current.density
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(EditorTab.FILTER) }

    LaunchedEffect(imagePath, density) { viewModel.load(imagePath, density) }
    LaunchedEffect(isPremium) { if (isPremium) viewModel.dismissPaywall() }
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { snackbar.showSnackbar("Image saved") }
    }
    LaunchedEffect(viewModel) {
        viewModel.notices.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppColors.background,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EditorTopBar(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onReset = viewModel::reset,
                    onSave = viewModel::save,
                )
                PreviewArea(
                    state = state,
                    onPressOriginal = viewModel::setShowOriginal,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                EditorTabRow(selected = tab, onSelect = { tab = it })
                Box(modifier = Modifier.fillMaxWidth().background(AppColors.surface)) {
                    val params = state.snapshot.params
                    when (tab) {
                        EditorTab.FILTER -> FilterPanel(
                            params = params,
                            filters = viewModel.filters,
                        isPremium = isPremium,
                            thumbnails = state.thumbnails,
                            onRequestThumbnail = viewModel::requestThumbnail,
                            onSelect = viewModel::selectFilter,
                            onLive = viewModel::updateParams,
                            onCommit = viewModel::commitParams,
                            recipes = recipes,
                            canSaveRecipe = state.sourceLoaded && !state.isSaving,
                            onSaveRecipe = viewModel::saveRecipe,
                            onApplyRecipe = viewModel::applyRecipe,
                            onImportRecipe = viewModel::importRecipeCode,
                        )
                        EditorTab.ADJUST -> AdjustPanel(
                            params = params,
                            onLive = viewModel::updateParams,
                            onCommit = viewModel::commitParams,
                        )
                        EditorTab.CROP -> CropPanel(crop = params.crop, onCrop = viewModel::setCrop)
                        EditorTab.FRAME -> FramePanel(frame = state.snapshot.frame, onFrame = viewModel::setFrame)
                        EditorTab.OVERLAY -> OverlayPanel(overlays = state.snapshot.overlays, onOverlays = viewModel::setOverlays)
                    }
                }
            }
        }

        if (state.showPaywall) {
            PaywallScreen(
                before = state.original,
                after = state.preview,
                monthlyPrice = prices[BillingRepository.MONTHLY_PRODUCT_ID],
                yearlyPrice = prices[BillingRepository.YEARLY_PRODUCT_ID],
                onPurchase = { productId -> activity?.let { billing.launchPurchase(it, productId) } },
                onDismiss = viewModel::dismissPaywall,
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    state: EditorState,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides AppColors.textPrimary) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = state.canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onRedo, enabled = state.canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = onReset, enabled = state.sourceLoaded) {
                Icon(Icons.Default.Restore, contentDescription = "Reset")
            }
            Spacer(Modifier.width(Spacing.sm))
            GlassButton(
                onClick = onSave,
                text = if (state.isSaving) "Saving" else "Save",
                enabled = state.sourceLoaded && !state.isSaving,
            )
        }
    }
}

/** Shows the edited preview. Holding a finger on it shows the original until release. */
@Composable
private fun PreviewArea(
    state: EditorState,
    onPressOriginal: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = if (state.showOriginal) state.original else state.preview
    Box(
        modifier = modifier
            .background(AppColors.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressOriginal(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onPressOriginal(false)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (shown != null) {
            val image = remember(shown) { shown.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = if (state.showOriginal) "Original photo" else "Edited photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(Spacing.lg),
            )
        } else {
            CircularProgressIndicator(color = AppColors.accent)
        }
        if (state.showOriginal) {
            Text(
                text = "Original",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.textPrimary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(Spacing.sm),
            )
        }
    }
}

@Composable
private fun EditorTabRow(selected: EditorTab, onSelect: (EditorTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        EditorTab.values().forEach { tab ->
            val color = if (tab == selected) AppColors.accent else AppColors.textSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable { onSelect(tab) }
                    .padding(vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(IconSize.md))
                Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}
