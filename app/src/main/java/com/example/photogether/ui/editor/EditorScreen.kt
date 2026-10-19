package com.example.photogether.ui.editor

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.net.Uri
import androidx.navigation.NavController
import com.example.photogether.core.extension.findActivity
import com.example.photogether.core.render.PolaroidGesture
import com.example.photogether.core.render.PolaroidMode
import com.example.photogether.core.render.PolaroidStyle
import com.example.photogether.core.render.PolaroidStyles
import com.example.photogether.core.render.RecipeCode
import com.example.photogether.core.share.launch as launchShareTarget
import com.example.photogether.data.billing.BillingRepository
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.components.ShareSheet
import com.example.photogether.ui.view.PaywallScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import org.koin.androidx.compose.get
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import kotlin.math.min
import com.example.photogether.ui.theme.AppIcons

/** Bottom tabs of the editor. */
enum class EditorTab(val label: String, val icon: ImageVector) {
    FILTER("Filter", AppIcons.Filter),
    ADJUST("Adjust", AppIcons.Adjust),
    CROP("Crop", AppIcons.Crop),
    FRAME("Frame", AppIcons.Frame),
    OVERLAY("Overlay", AppIcons.Sparkle),
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val billing: BillingRepository = get()
    val prices by billing.prices.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val density = LocalDensity.current.density
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(EditorTab.FILTER) }

    LaunchedEffect(imagePath, density) { viewModel.load(imagePath, density) }
    LaunchedEffect(isPremium) { if (isPremium) viewModel.dismissPaywall() }
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
                    placementEnabled = tab == EditorTab.FRAME && state.snapshot.frame.polaroid == PolaroidMode.FILL,
                    onPlacementInteract = viewModel::setFrameInteracting,
                    onPlacementDrag = viewModel::dragPlacement,
                    onPlacementCommit = viewModel::commitFrame,
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
                            favorites = state.favorites,
                            onToggleFavorite = viewModel::toggleFavorite,
                        )
                        EditorTab.ADJUST -> AdjustPanel(
                            params = params,
                            onLive = viewModel::updateParams,
                            onCommit = viewModel::commitParams,
                        )
                        EditorTab.CROP -> CropPanel(crop = params.crop, onCrop = viewModel::setCrop)
                        EditorTab.FRAME -> FramePanel(
                            frame = state.snapshot.frame,
                            onFrame = viewModel::setFrame,
                            onFrameLive = { frame -> viewModel.updateFrameLive { frame } },
                            onCommit = viewModel::commitFrame,
                        )
                        EditorTab.OVERLAY -> OverlayPanel(overlays = state.snapshot.overlays, onOverlays = viewModel::setOverlays)
                    }
                }
            }
        }

        if (state.showShareSheet) {
            val savedUri = state.lastSavedUri
            ShareSheet(
                previewUri = savedUri,
                recipeCode = RecipeCode.encode(state.snapshot.params),
                onTarget = { target ->
                    savedUri?.let { launchShareTarget(context, Uri.parse(it), target) }
                },
                onSaveRecipe = viewModel::saveRecipe,
                onDismiss = viewModel::dismissShareSheet,
            )
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
                Icon(AppIcons.Back, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = state.canUndo) {
                Icon(AppIcons.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onRedo, enabled = state.canRedo) {
                Icon(AppIcons.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = onReset, enabled = state.sourceLoaded) {
                Icon(AppIcons.Reset, contentDescription = "Reset")
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

/**
 * Shows the edited preview. Holding a finger on it shows the original until release.
 * When [placementEnabled] (Frame tab, FILL Polaroid) a drag pans the photo and a pinch zooms it.
 * Each gesture ends with [onPlacementCommit] as one undo step. The hold-to-see-original is off then.
 */
@Composable
private fun PreviewArea(
    state: EditorState,
    onPressOriginal: (Boolean) -> Unit,
    placementEnabled: Boolean,
    onPlacementInteract: (Boolean) -> Unit,
    onPlacementDrag: (panPxX: Float, panPxY: Float, zoomBy: Float, windowSidePx: Float) -> Unit,
    onPlacementCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = if (state.showOriginal) state.original else state.preview
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val windowSide = placementWindowSide(
        size = imageSize,
        shown = shown,
        style = PolaroidStyles.byId(state.snapshot.frame.polaroidStyleId),
    )
    // Read the latest values inside the gesture loop without restarting it mid-drag.
    val currentWindowSide by rememberUpdatedState(windowSide)
    val currentDrag by rememberUpdatedState(onPlacementDrag)
    val currentInteract by rememberUpdatedState(onPlacementInteract)
    val currentCommit by rememberUpdatedState(onPlacementCommit)

    Box(
        modifier = modifier
            .background(AppColors.background)
            .pointerInput(placementEnabled) {
                if (placementEnabled) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        currentInteract(true)
                        do {
                            val event: PointerEvent = awaitPointerEvent()
                            val zoomBy = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoomBy != 1f || pan != Offset.Zero) {
                                event.changes.forEach { it.consume() }
                                currentDrag(pan.x, pan.y, zoomBy, currentWindowSide)
                            }
                        } while (event.changes.any { it.pressed })
                        currentInteract(false)
                        currentCommit()
                    }
                } else {
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
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (shown != null) {
            val image = remember(shown) { shown.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = if (state.showOriginal) "Original photo" else "Edited photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg)
                    .onSizeChanged { imageSize = it },
            )
        } else {
            CircularProgressIndicator(color = AppColors.accent)
        }
        if (state.showOriginal) {
            // Sits over the photo, so white ink on a black scrim.
            Text(
                text = stringResource(R.string.editor_original),
                style = MaterialTheme.typography.labelMedium,
                color = InkColors.textPrimary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(Spacing.sm)
                    .background(InkColors.background.copy(alpha = 0.6f), RoundedCornerShape(Radius.sm))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }
    }
}

/**
 * Side in screen pixels of the Polaroid photo window. The preview bitmap is letterboxed into [size]
 * (ContentScale.Fit), so the window is measured from the drawn size, not the whole box.
 * Returns 0 until the box has been measured.
 */
private fun placementWindowSide(size: IntSize, shown: Bitmap?, style: PolaroidStyle): Float {
    if (shown == null || shown.width <= 0 || shown.height <= 0) return 0f
    if (size.width <= 0 || size.height <= 0) return 0f
    val scale = min(size.width.toFloat() / shown.width, size.height.toFloat() / shown.height)
    return PolaroidGesture.windowSidePx(shown.width * scale, shown.height * scale, style)
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
