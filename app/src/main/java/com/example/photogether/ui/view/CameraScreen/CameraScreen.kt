package com.example.photogether.ui.view.CameraScreen

import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavController
import com.example.photogether.core.camera.CameraTool
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.core.render.CameraProfiles
import com.example.photogether.core.render.FilterCycle
import com.example.photogether.core.render.RecipeFilters
import com.example.photogether.ui.components.CameraAdjustPanel
import com.example.photogether.ui.components.CameraQuickBar
import com.example.photogether.ui.components.CameraToolButtons
import com.example.photogether.ui.components.FilterStrip
import com.example.photogether.ui.components.FocusIndicator
import com.example.photogether.ui.components.GridOverlay
import com.example.photogether.ui.components.ManualControls
import com.example.photogether.ui.components.ZoomControl
import com.example.photogether.ui.components.ZoomPill
import com.example.photogether.ui.camera.PhotoFlight
import com.example.photogether.ui.camera.PhotoFlightOverlay
import com.example.photogether.ui.components.kelvinLabel
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Motion
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.viewmodel.CameraViewModel
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

private const val TAG = "CameraScreen"
private const val FILTER_NAME_HOLD_MS = 900L

/** One filter-name flash. A new instance per selection, so the same name flashes again. */
class NameFlash(val name: String)

// region Compose
@Composable
fun CameraScreen(
    navController: NavController,
    cameraViewModel: CameraViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val exposureTime = remember { mutableLongStateOf(10_000_000L) }
    val isoValue = remember { mutableIntStateOf(1000) }
    val recipes by cameraViewModel.recipes.collectAsState(initial = emptyList())
    val allSources = remember(recipes) {
        cameraViewModel.catalog.filters + recipes.map { RecipeFilters.toFilterInfo(it) }
    }
    // The highlighted source. A recipe id is not a catalog id, so it is tracked here.
    var selectedId by rememberSaveable { mutableStateOf(cameraViewModel.editParams.value.filterId) }
    // The chosen camera. The strip lists only its filters, and recipes built on one of them.
    var cameraId by rememberSaveable {
        mutableStateOf(CameraProfiles.forFilter(selectedId)?.id ?: CameraProfiles.ALL.first().id)
    }
    val cameraProfile = CameraProfiles.byId(cameraId)
    val sources = remember(allSources, recipes, cameraProfile) {
        val own = cameraProfile.filterIds.mapNotNull { id -> allSources.firstOrNull { it.id == id } }
        own + recipes.filter { it.params.filterId in cameraProfile.filterIds }.map { RecipeFilters.toFilterInfo(it) }
    }
    var flash by remember { mutableStateOf<NameFlash?>(null) }
    val adjust by cameraViewModel.adjust.collectAsStateWithLifecycle()
    val camera = cameraViewModel.camera.value
    // The gallery box shows the last photo taken this session. A new photo flies from the viewfinder
    // into the box; both are measured in window px so the flight lands exactly on the box.
    // rememberSaveable: the screen leaves composition when the picker opens, and a plain remember would reset the box.
    var lastPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var flight by remember { mutableStateOf<PhotoFlight?>(null) }
    var viewfinderBounds by remember { mutableStateOf<Rect?>(null) }
    var galleryBounds by remember { mutableStateOf<Rect?>(null) }

    val onSelect: (String) -> Unit = { id ->
        val recipe = RecipeFilters.recipeFor(id, recipes)
        if (recipe != null) {
            cameraViewModel.setFilter(recipe.params)
        } else {
            cameraViewModel.selectFilter(id)
        }
        selectedId = id
        flash = NameFlash(allSources.firstOrNull { it.id == id }?.name.orEmpty())
    }
    val onSwipe: (Int) -> Unit = { delta ->
        onSelect(FilterCycle.next(sources.map { it.id }, selectedId, delta))
    }
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(lifecycleOwner) {
        cameraViewModel.initCamera(lifecycleOwner, CameraSelector.LENS_FACING_BACK)
    }

    // The picker opens on the camera in use.
    LaunchedEffect(navController, cameraId) {
        navController.currentBackStackEntry?.savedStateHandle?.set(CameraProfiles.CURRENT_KEY, cameraId)
    }

    // The camera picker returns its chosen camera id on this screen's back-stack entry. Entries are
    // observed, so a result is handled again after the picker pops back. Choosing a camera applies its
    // first filter, unless the current filter already belongs to it.
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            entry.savedStateHandle.getStateFlow<String?>(CameraProfiles.RESULT_KEY, null).collect { id ->
                if (id != null) {
                    val picked = CameraProfiles.byId(id)
                    cameraId = picked.id
                    currentOnSelect(picked.filterId)
                    // Picking a camera resets the film-effect fields to the camera's recipe.
                    cameraViewModel.applyCameraRecipe(picked.recipe)
                    entry.savedStateHandle.remove<String>(CameraProfiles.RESULT_KEY)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(InkColors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            CameraTopBar(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
            )

            // The viewfinder takes the space the rest of the screen leaves. Its bottom overlays sit inside it.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            ) {
                CameraPreviewViewfinder(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { viewfinderBounds = it.boundsInRoot() },
                    previewUseCase = cameraViewModel.preview.value,
                    camera = camera,
                    flash = flash,
                    onSwipe = onSwipe,
                    onFocusPoint = cameraViewModel::focusAt,
                )
                // Overlays the bottom edge of the viewfinder. Kept outside the viewfinder's swipe box so
                // slider drags do not also cycle filters.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    camera?.let { bound ->
                        ZoomControl(cameraControl = bound.cameraControl, cameraInfo = bound.cameraInfo)
                    }
                    CameraQuickBar(
                        state = adjust,
                        onToolClick = cameraViewModel::toggleTool,
                        zoomPill = {
                            camera?.let {
                                ZoomPill(
                                    cameraInfo = it.cameraInfo,
                                    lensMm = cameraViewModel.lensMm.floatValue,
                                    onTap = cameraViewModel::setFocal,
                                )
                            }
                        },
                    )
                }
            }

            // Readout: the Kelvin value in mono, plus the EV reset button while the EV panel is open.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            ) {
                Text(
                    text = kelvinLabel(adjust.kelvin).uppercase(),
                    style = BinsooType.label,
                    color = InkColors.textPrimary,
                )
                if (adjust.openTool == CameraTool.EXPOSURE) {
                    EvResetButton(
                        onClick = {
                            cameraViewModel.resetEv()
                            cameraViewModel.commitAdjust()
                        },
                    )
                }
            }

            // Ruler (only while a tool is open) beside the round tool buttons.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CameraAdjustPanel(
                    state = adjust,
                    onKelvinChange = { cameraViewModel.setKelvin(it) },
                    onKelvinAuto = cameraViewModel::setKelvinAuto,
                    onEvChange = cameraViewModel::setEv,
                    onCommit = cameraViewModel::commitAdjust,
                    modifier = Modifier.weight(1f),
                )
                CameraToolButtons(
                    state = adjust,
                    onAuto = cameraViewModel::setKelvinAuto,
                    onToolClick = cameraViewModel::toggleTool,
                )
            }

            FilterStrip(
                filters = sources,
                selectedId = selectedId,
                onSelect = onSelect,
                modifier = Modifier.padding(vertical = Spacing.sm),
                dark = true,
            )

            ManualControls(
                onExposureChange = {
                    exposureTime.longValue = it
                    cameraViewModel.setManualExposure(it, isoValue.intValue)
                },
                onISOChange = {
                    isoValue.intValue = it
                    cameraViewModel.setManualExposure(exposureTime.longValue, it)
                },
                onClickCapture = {
                    cameraViewModel.takePhotoToGallery(
                        onSaved = { uri ->
                            // A flight still in the air lands now, so its photo is not lost.
                            flight?.let { lastPhoto = it.uri }
                            val from = viewfinderBounds
                            val to = galleryBounds
                            if (from != null && to != null) {
                                flight = PhotoFlight(uri, from, to)
                            } else {
                                lastPhoto = uri
                            }
                        },
                        onError = { exception -> Log.e(TAG, "Capture failed", exception) },
                    )
                },
                onOpenGallery = { navController.navigate(AppRoutes.PHOTO_PICKER) },
                onOpenPicker = { navController.navigate(AppRoutes.CAMERA_PICKER) },
                pickerName = cameraProfile.name,
                lastPhoto = lastPhoto,
                onGalleryBoundsChanged = { galleryBounds = it },
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        }
        flight?.let { current ->
            PhotoFlightOverlay(flight = current) {
                lastPhoto = current.uri
                flight = null
            }
        }
    }
}

/** Back arrow on the left, settings (hamburger) on the right. */
@Composable
private fun CameraTopBar(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = InkColors.textPrimary,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Cài đặt",
                tint = InkColors.textPrimary,
            )
        }
    }
}

/**
 * The rounded viewfinder (28dp corners): the preview, the filter-name flash, the grid, the focus ring
 * and the grid toggle. The zoom and tool pills are drawn by [CameraScreen] over its bottom edge.
 *
 * [navController] and [camera] are kept so the public signature does not change. They are not used
 * here now that the gallery and picker buttons sit in the bottom row and the zoom is drawn by the screen.
 */
@Composable
fun CameraPreviewViewfinder(
    navController: NavController,
    modifier: Modifier,
    previewUseCase: Preview?,
    camera: Camera?,
    flash: NameFlash?,
    onSwipe: (Int) -> Unit,
    onFocusPoint: (MeteringPoint) -> Unit = {},
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnFocusPoint by rememberUpdatedState(onFocusPoint)
    // Last tap position in the viewfinder, and a counter that replays the focus ring on each tap.
    var focusPos by remember { mutableStateOf<Offset?>(null) }
    var focusTrigger by remember { mutableIntStateOf(0) }
    var showGrid by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(previewUseCase) {
        previewUseCase?.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.viewfinder))
            .background(InkColors.background)
            .pointerInput(Unit) {
                val threshold = Spacing.xxl.toPx()
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (abs(total) >= threshold) currentOnSwipe(if (total < 0f) 1 else -1)
                    },
                    onDragCancel = { total = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                    },
                )
            }
    ) {
        // Tap-to-focus lives on its own child so the grid button, a sibling, never reaches it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            currentOnFocusPoint(previewView.meteringPointFactory.createPoint(offset.x, offset.y))
                            focusPos = offset
                            focusTrigger++
                        },
                    )
                },
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            FilterNameFlash(flash)
            if (showGrid) GridOverlay()
            FocusIndicator(position = focusPos, trigger = focusTrigger)
        }
        GridToggle(
            on = showGrid,
            onToggle = { showGrid = !showGrid },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.sm),
        )
    }
}

/** Resets exposure compensation to 0 EV. Dark pill with a reset arrow, shown beside the Kelvin readout. */
@Composable
private fun EvResetButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .semantics { contentDescription = "Đặt lại EV về 0" }
            .clip(RoundedCornerShape(percent = 50))
            .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "↺",
            style = MaterialTheme.typography.labelMedium,
            color = InkColors.textPrimary,
        )
    }
}

/** Grid on/off button. Draws a small 3x3 grid icon; the tint and fill change when the grid is on. */
@Composable
private fun GridToggle(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .semantics { contentDescription = if (on) "Tắt lưới" else "Bật lưới" }
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (on) InkColors.accent else InkColors.surfaceElevated.copy(alpha = 0.7f)
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val tint = if (on) InkColors.onAccent else InkColors.textPrimary
            val stroke = 1.dp.toPx()
            for (i in 1..2) {
                val x = size.width * i / 3f
                drawLine(tint, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                val y = size.height * i / 3f
                drawLine(tint, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
        }
    }
}

/** Centre-of-preview filter name that fades out after a short hold. */
@Composable
private fun BoxScope.FilterNameFlash(flash: NameFlash?) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(flash) {
        if (flash == null) return@LaunchedEffect
        fade.snapTo(1f)
        delay(FILTER_NAME_HOLD_MS)
        fade.animateTo(0f, tween(Motion.DurationLong))
    }
    Text(
        text = flash?.name.orEmpty(),
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer { alpha = fade.value },
        style = MaterialTheme.typography.headlineSmall,
        color = InkColors.textPrimary,
    )
}

@ComposePreview
@Composable
fun ManualControlsPreview() {
    ManualControls(onExposureChange = {}, onISOChange = {}, onClickCapture = {})
}
// endregion
