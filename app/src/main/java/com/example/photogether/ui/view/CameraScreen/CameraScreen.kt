package com.example.photogether.ui.view.CameraScreen

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.photogether.core.render.FlashMode
import com.example.photogether.core.render.FlashStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.text.drawText
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavController
import com.example.photogether.core.camera.CameraTool
import com.example.photogether.core.camera.formatEv
import com.example.photogether.core.camera.lensOptions
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.core.render.CameraAspect
import com.example.photogether.core.render.CameraProfiles
import com.example.photogether.core.render.FilterCatalog
import com.example.photogether.core.render.CropMath
import com.example.photogether.core.render.RecipeFilters
import com.example.photogether.ui.components.AspectButton
import com.example.photogether.ui.components.AspectPanel
import com.example.photogether.ui.components.CameraAdjustPanel
import com.example.photogether.ui.components.CameraCircleButton
import com.example.photogether.ui.components.CameraQuickBar
import com.example.photogether.ui.components.FilterStrip
import com.example.photogether.ui.components.CameraToolButtons
import com.example.photogether.ui.components.FocalPill
import com.example.photogether.ui.components.FocusIndicator
import com.example.photogether.ui.components.GridOverlay
import com.example.photogether.ui.components.LensPanel
import com.example.photogether.ui.components.ManualControls
import com.example.photogether.ui.components.SUN_PRESET_KELVIN
import com.example.photogether.ui.components.BULB_PRESET_KELVIN
import com.example.photogether.ui.components.kelvinLabel
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Motion
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.viewmodel.CameraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import com.example.photogether.ui.theme.AppIcons

private const val TAG = "CameraScreen"
private const val FILTER_NAME_HOLD_MS = 900L

/** Height of the panel slot under the viewfinder. Fixed, so opening a panel never resizes the viewfinder. Sized for the aspect panel. */
private val PANEL_SLOT_HEIGHT = 120.dp

/** The one panel open under the viewfinder. WB and EV also drive the view model's open tool. */
private enum class CameraPanel { NONE, ASPECT, WB, LENS, EV }

/** One screen flash at capture. A new instance per shot, so every shot replays it. */
class FlashFx(val style: FlashStyle)

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
    // The camera picked last, kept in DataStore. Before any pick, the camera that owns the current filter.
    val savedCameraId by cameraViewModel.savedCameraId.collectAsState(initial = null)
    val cameraId = savedCameraId ?: CameraProfiles.forFilter(selectedId)?.id
    val cameraProfile = cameraId?.let { CameraProfiles.byId(it) }
    // The strip is the original, then the chosen camera's filters. Recipes built on one of those show too.
    val stripIds = CameraProfiles.stripIds(cameraProfile)
    val sources = remember(allSources, recipes, stripIds) {
        val own = stripIds.mapNotNull { id -> allSources.firstOrNull { it.id == id } }
        own + recipes.filter { it.params.filterId in stripIds }.map { RecipeFilters.toFilterInfo(it) }
    }
    var flash by remember { mutableStateOf<NameFlash?>(null) }
    val adjust by cameraViewModel.adjust.collectAsStateWithLifecycle()
    val camera = cameraViewModel.camera.value
    val zoomState = camera?.cameraInfo?.zoomState?.observeAsState()?.value
    val cameraZoom = zoomState?.zoomRatio ?: 1f
    val maxZoom = zoomState?.maxZoomRatio ?: 1f
    val cropZoomMm = (cameraViewModel.lensMm.floatValue * cameraZoom * cameraViewModel.cropZoom.floatValue).roundToInt()
    // The open panel under the viewfinder. Only one is open at a time.
    var panel by rememberSaveable { mutableStateOf(CameraPanel.NONE) }
    // The aspect in use: the one the user picked, or the full 3:4 frame.
    val effectiveAspect = cameraViewModel.aspectOverride.value ?: CameraAspect.FOUR_THREE
    // One PreviewView for the life of the screen, so its last frame can be captured before a ratio change.
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    // Last live frame, shown while the ratio changes. The live view is paused behind it, so the layout change
    // never shows as a stretched or stuttering preview. Null when live.
    var frozenFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    // The viewfinder's shape glides to the new ratio; the frozen frame is dropped once it settles.
    val viewRatio by animateFloatAsState(
        targetValue = effectiveAspect.widthOverHeight,
        animationSpec = tween(durationMillis = 280),
        finishedListener = { frozenFrame = null },
        label = "viewfinderRatio",
    )
    val onSelectAspect: (CameraAspect) -> Unit = { aspect ->
        if (aspect != effectiveAspect) {
            frozenFrame = previewView.bitmap?.asImageBitmap()
            cameraViewModel.setAspect(aspect)
        }
    }
    // Opens [target], or closes it when it is already open. WB and EV set the view model's open tool to match,
    // because CameraAdjustPanel reads that tool to choose its ruler.
    val openPanel: (CameraPanel) -> Unit = { target ->
        val next = if (panel == target) CameraPanel.NONE else target
        panel = next
        val wanted = when (next) {
            CameraPanel.WB -> CameraTool.WHITE_BALANCE
            CameraPanel.EV -> CameraTool.EXPOSURE
            else -> null
        }
        if (adjust.openTool != wanted) {
            adjust.openTool?.let { cameraViewModel.toggleTool(it) }
            wanted?.let { cameraViewModel.toggleTool(it) }
        }
    }
    val flashMode by cameraViewModel.flashMode.collectAsStateWithLifecycle()
    var flashFx by remember { mutableStateOf<FlashFx?>(null) }
    // The gallery iris shows the last photo taken this session. captureSeq counts shutter presses, so the
    // iris closes on each one and reopens on the new photo.
    // rememberSaveable: the screen leaves composition when the gallery opens, and a plain remember would reset the box.
    var lastPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var captureSeq by remember { mutableIntStateOf(0) }

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
    // Vertical drag on the viewfinder sets the strength of the current filter. The fraction is of the viewfinder height.
    val onStrength: (Float) -> Unit = { delta ->
        val percent = (cameraViewModel.adjustFilterIntensity(delta) * 100).roundToInt()
        flash = NameFlash("Cường độ $percent%")
    }
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(lifecycleOwner) {
        cameraViewModel.initCamera(lifecycleOwner, CameraSelector.LENS_FACING_BACK)
    }

    // Captures crop to the active camera's aspect.
    // The shutter sound is set on the Settings screen. Re-read it whenever this screen resumes, so a change made there applies.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraViewModel.reloadShutterSound()
                cameraViewModel.reloadCropZoom()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Runs for the first camera and every change after. A ratio picked for the previous camera does not carry over.
    LaunchedEffect(cameraProfile) {
        cameraViewModel.aspectOverride.value = null
        cameraViewModel.setCameraAspect(CameraAspect.FOUR_THREE)
        cameraViewModel.setFlashMode(FlashMode.OFF)
    }

    // A camera saved by the picker (from Home or from here) applies its first filter, unless the current
    // filter already belongs to it.
    LaunchedEffect(savedCameraId) {
        val picked = savedCameraId?.let { CameraProfiles.byId(it) } ?: return@LaunchedEffect
        if (selectedId !in picked.filterIds) currentOnSelect(picked.filterId)
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
            // Its shape follows the chosen aspect, so the live view shows the same crop as the capture.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                // Bottom-aligned so the frame sits right above the ratio row, with the spare space above it.
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(modifier = Modifier.aspectRatio(viewRatio)) {
                    CameraPreviewViewfinder(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize(),
                        previewView = previewView,
                        previewUseCase = cameraViewModel.preview.value,
                        camera = camera,
                        flash = flash,
                        aspect = effectiveAspect,
                        cropZoom = cameraViewModel.cropZoom.floatValue,
                        cropZoomOn = cameraViewModel.cropZoomActive.value,
                        zoomMm = cropZoomMm,
                        onStrength = onStrength,
                        onFocusPoint = cameraViewModel::focusAt,
                        onPinch = cameraViewModel::pinchZoom,
                        onPinchEnd = cameraViewModel::endPinch,
                    )
                    // Covers the live view while the ratio changes, so the resize is never seen.
                    frozenFrame?.let { frame ->
                        Image(
                            bitmap = frame,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(Radius.viewfinder)),
                        )
                    }
                    ScreenFlash(
                        fx = flashFx,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(Radius.viewfinder)),
                    )
                    // Overlays the bottom edge of the viewfinder. Kept outside the viewfinder's swipe box so
                    // slider drags do not also cycle filters.
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CameraQuickBar(
                            kelvin = adjust.kelvin,
                            ev = adjust.ev,
                            wbOpen = panel == CameraPanel.WB,
                            evOpen = panel == CameraPanel.EV,
                            onWbClick = { openPanel(CameraPanel.WB) },
                            onEvClick = { openPanel(CameraPanel.EV) },
                            focalPill = {
                                camera?.let {
                                    FocalPill(
                                        cameraInfo = it.cameraInfo,
                                        lensMm = cameraViewModel.lensMm.floatValue,
                                        cropZoom = cameraViewModel.cropZoom.floatValue,
                                        selected = panel == CameraPanel.LENS,
                                        onClick = { openPanel(CameraPanel.LENS) },
                                    )
                                }
                            },
                        )
                    }
                }
            }

            // Panel slot under the viewfinder. Its content follows [panel]. The height is fixed, so the viewfinder never resizes.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PANEL_SLOT_HEIGHT)
                    .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                when (panel) {
                    CameraPanel.NONE -> Row(
                        // Top of the slot, right under the frame; the panels below keep their centred layout.
                        modifier = Modifier.align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
                    ) {
                        // Flash is off on the front camera, so its button is dimmed and does nothing there.
                        val flashAvailable = cameraViewModel.hasFlash.value
                        CameraCircleButton(
                            icon = flashIcon(flashMode),
                            label = stringResource(R.string.camera_flash_fmt, flashLabel(flashMode)),
                            onClick = { if (flashAvailable) cameraViewModel.setFlashMode(flashMode.next()) },
                            buttonSize = 44.dp,
                            contentColor = if (flashAvailable) InkColors.textPrimary else InkColors.textSecondary,
                        )
                        AspectButton(
                            label = effectiveAspect.label,
                            onClick = { openPanel(CameraPanel.ASPECT) },
                        )
                        CameraCircleButton(
                            icon = AppIcons.SwitchCamera,
                            label = stringResource(R.string.camera_switch),
                            onClick = cameraViewModel::switchFacing,
                            buttonSize = 44.dp,
                        )
                    }
                    CameraPanel.ASPECT -> AspectPanel(
                        selected = effectiveAspect,
                        onSelect = onSelectAspect,
                        onClose = { openPanel(CameraPanel.ASPECT) },
                    )
                    CameraPanel.WB -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = kelvinLabel(adjust.kelvin).uppercase(),
                            style = BinsooType.label,
                            color = InkColors.textPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                onAuto = {
                                    cameraViewModel.setKelvinAuto()
                                    cameraViewModel.commitAdjust()
                                },
                                onSun = {
                                    cameraViewModel.setKelvin(SUN_PRESET_KELVIN)
                                    cameraViewModel.commitAdjust()
                                },
                                onBulb = {
                                    cameraViewModel.setKelvin(BULB_PRESET_KELVIN)
                                    cameraViewModel.commitAdjust()
                                },
                            )
                        }
                    }
                    CameraPanel.EV -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = formatEv(adjust.ev),
                            style = BinsooType.label,
                            color = InkColors.textPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                            CameraCircleButton(
                                icon = AppIcons.Undo,
                                label = stringResource(R.string.camera_reset_ev),
                                onClick = {
                                    cameraViewModel.resetEv()
                                    cameraViewModel.commitAdjust()
                                },
                            )
                        }
                    }
                    CameraPanel.LENS -> LensPanel(
                        options = lensOptions(cameraViewModel.availableLenses.value, cameraViewModel.mainMm.floatValue, maxZoom),
                        currentMm = cropZoomMm.toFloat(),
                        mainMm = cameraViewModel.mainMm.floatValue,
                        onSelect = { cameraViewModel.setFocal(it.roundToInt()) },
                    )
                }
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
                    // Flash and iris only play when the press is accepted (a capture in progress ignores it).
                    val accepted = cameraViewModel.takePhotoToGallery(
                        cameraId = cameraProfile?.id ?: FilterCatalog.ORIGINAL_ID,
                        onSaved = { uri -> lastPhoto = uri },
                        onError = { exception ->
                            Log.e(TAG, "Capture failed", exception)
                            Toast.makeText(context, context.getString(R.string.camera_save_failed), Toast.LENGTH_SHORT).show()
                        },
                    )
                    if (accepted) {
                        if (flashMode == FlashMode.ON && cameraViewModel.hasFlash.value) {
                            flashFx = FlashFx(FlashStyle.HARSH)
                        }
                        captureSeq++
                    }
                },
                onOpenGallery = { navController.navigate(AppRoutes.CAMERA_GALLERY) },
                onOpenPicker = { navController.navigate(AppRoutes.CAMERA_PICKER) },
                pickerName = cameraProfile?.name,
                pickerCameraId = cameraProfile?.id,
                lastPhoto = lastPhoto,
                captureSeq = captureSeq,
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        }
    }
}

/** Icon for the flash button: the current [FlashMode]. */
private fun flashIcon(mode: FlashMode): ImageVector = when (mode) {
    FlashMode.OFF -> AppIcons.FlashOff
    FlashMode.AUTO -> AppIcons.FlashAuto
    FlashMode.ON -> AppIcons.FlashOn
    FlashMode.TORCH -> AppIcons.Torch
}

/** Name of the current [FlashMode], used in the button's accessibility label. */
@Composable
private fun flashLabel(mode: FlashMode): String = stringResource(
    when (mode) {
        FlashMode.OFF -> R.string.flash_off
        FlashMode.AUTO -> R.string.flash_auto
        FlashMode.ON -> R.string.flash_on
        FlashMode.TORCH -> R.string.flash_torch
    }
)

/** Back arrow on the left, settings (hamburger) on the right. */
@Composable
private fun CameraTopBar(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = stringResource(R.string.action_back),
                tint = InkColors.textPrimary,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = AppIcons.Menu,
                contentDescription = stringResource(R.string.settings_title),
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
    previewView: PreviewView,
    previewUseCase: Preview?,
    camera: Camera?,
    flash: NameFlash?,
    aspect: CameraAspect = CameraAspect.FOUR_THREE,
    cropZoom: Float = 1f,
    cropZoomOn: Boolean = false,
    zoomMm: Int = 0,
    onStrength: (delta: Float) -> Unit,
    onFocusPoint: (MeteringPoint) -> Unit = {},
    onPinch: (factor: Float) -> Unit = {},
    onPinchEnd: () -> Unit = {},
) {
    val currentOnStrength by rememberUpdatedState(onStrength)
    val currentOnFocusPoint by rememberUpdatedState(onFocusPoint)
    val currentOnPinch by rememberUpdatedState(onPinch)
    val currentOnPinchEnd by rememberUpdatedState(onPinchEnd)
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
                // Two fingers zoom. The zoom is applied on each change and the gesture ends when every finger lifts.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pinching = false
                    do {
                        val event = awaitPointerEvent()
                        val zoomBy = event.calculateZoom()
                        if (zoomBy != 1f) {
                            pinching = true
                            event.changes.forEach { it.consume() }
                            currentOnPinch(zoomBy)
                        }
                    } while (event.changes.any { it.pressed })
                    if (pinching) currentOnPinchEnd()
                }
            }
            .pointerInput(Unit) {
                // Dragging up raises the strength, dragging down lowers it. A full-height drag covers the whole range.
                detectVerticalDragGestures(
                    onDragStart = {},
                    onDragEnd = {},
                    onDragCancel = {},
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (size.height > 0) currentOnStrength(-dragAmount / size.height)
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
            // In crop-frame mode the frame's own scrim replaces the aspect mask.
            if (aspect.crops && !cropZoomOn) AspectMask(aspect.widthOverHeight)
            // At 1x the frame would only outline the whole view, so it shows only once the crop is zoomed in.
            if (cropZoomOn && cropZoom > 1f) CropZoomFrame(aspect.widthOverHeight.takeIf { aspect.crops }, cropZoom, "${zoomMm}mm")
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

/**
 * Darkens the viewfinder outside the saved crop. The crop is placed with the same FILL_CENTER rule PreviewView
 * uses for the 3:4 camera frame ([CropMath.visibleCrop]), so the mask matches the capture on any viewfinder size.
 */
@Composable
private fun AspectMask(ratio: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val r = CropMath.visibleCrop(w, h, CameraAspect.FOUR_THREE.widthOverHeight, ratio)
        val scrim = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
        fun band(l: Float, t: Float, rr: Float, b: Float) {
            if (rr > l && b > t) drawRect(scrim, Offset(l, t), androidx.compose.ui.geometry.Size(rr - l, b - t))
        }
        band(0f, 0f, w, r.top)
        band(0f, r.bottom, w, h)
        band(0f, r.top, r.left, r.bottom)
        band(r.right, r.top, w, r.bottom)
    }
}

/**
 * White rounded frame on the preview marking the part that a crop-frame zoom of [cropZoom] will capture, with
 * [mmLabel] centred above it. The frame starts from the same rect as [AspectMask] ([aspectRatio] null means the
 * whole camera frame) and shrinks about its centre.
 */
@Composable
private fun CropZoomFrame(aspectRatio: Float?, cropZoom: Float, mmLabel: String) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = androidx.compose.ui.text.TextStyle(
        color = androidx.compose.ui.graphics.Color.White,
        fontSize = 13.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
    )
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val ratio = aspectRatio ?: CameraAspect.FOUR_THREE.widthOverHeight
        // Zoom the unclipped crop (what the capture keeps), then clip to the view.
        val r = CropMath.clipTo(
            CropMath.zoomCrop(CropMath.unclippedCrop(w, h, CameraAspect.FOUR_THREE.widthOverHeight, ratio), cropZoom),
            w, h,
        )
        val topLeft = Offset(r.left, r.top)
        val frameSize = androidx.compose.ui.geometry.Size(r.width, r.height)
        val radius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
        // Light scrim outside the frame: the frame rect is cut out of a full-size path.
        val scrim = androidx.compose.ui.graphics.Path().apply {
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
            addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(topLeft, frameSize), radius))
        }
        drawPath(scrim, androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
        drawRoundRect(
            androidx.compose.ui.graphics.Color.White, topLeft, frameSize, radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
        )
        val label = measurer.measure(mmLabel, labelStyle)
        val lx = (r.left + r.right) / 2f - label.size.width / 2f
        val ly = (r.top - label.size.height - 6.dp.toPx()).coerceAtLeast(0f)
        drawText(label, topLeft = Offset(lx, ly))
    }
}

/** Short colour flash over the preview when a shot fires with flash. HARSH is white and quick, WARM is amber and slower. */
@Composable
private fun ScreenFlash(fx: FlashFx?, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(fx) {
        val style = fx?.style ?: return@LaunchedEffect
        val (peak, ms) = when (style) {
            FlashStyle.HARSH -> 0.85f to 120
            FlashStyle.WARM -> 0.5f to 250
            FlashStyle.NONE -> return@LaunchedEffect
        }
        alpha.snapTo(peak)
        alpha.animateTo(0f, tween(ms))
    }
    val color = if (fx?.style == FlashStyle.WARM) Color(0xFFFFB347) else Color.White
    if (alpha.value > 0f) {
        Box(modifier.background(color.copy(alpha = alpha.value)))
    }
}
