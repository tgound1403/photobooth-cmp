package com.example.photogether.ui.view.CameraScreen

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.example.photogether.core.navigation.navigateOnce
import com.example.photogether.core.navigation.popBackStackOnce
import com.example.photogether.ui.components.CAMERA_BUTTON_SIZE
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.photogether.ui.components.FanUiState
import com.example.photogether.ui.components.FilterFanOverlay
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
import com.example.photogether.core.camera.VolumeKeyCapture
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.example.photogether.ui.camera.CameraBodyPlate
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
import com.example.photogether.core.camera.ExposureOverlayPrefs
import com.example.photogether.core.camera.GridPrefs
import com.example.photogether.core.camera.ManualExposureSteps
import com.example.photogether.core.camera.CameraAdjustState
import com.example.photogether.core.camera.MeasuredExposure
import com.example.photogether.core.camera.RotateTipPrefs
import com.example.photogether.core.camera.RoundWheelPrefs
import com.example.photogether.ui.components.FilterFanButton
import com.example.photogether.ui.components.RotateTip
import com.example.photogether.core.render.CameraAspect
import com.example.photogether.core.render.CameraProfiles
import com.example.photogether.core.render.FilterCatalog
import com.example.photogether.core.render.CropMath
import com.example.photogether.core.render.RecipeFilters
import com.example.photogether.ui.components.AspectButton
import com.example.photogether.ui.components.AspectPanel
import com.example.photogether.ui.components.CameraAdjustPanel
import com.example.photogether.core.tutorial.TutorialScreen
import com.example.photogether.core.tutorial.TutorialStep
import com.example.photogether.ui.tutorial.TutorialLayer
import com.example.photogether.ui.tutorial.TutorialViewModel
import com.example.photogether.ui.tutorial.tutorialTarget
import com.example.photogether.ui.components.CameraCircleButton
import com.example.photogether.ui.components.CameraQuickBar
import com.example.photogether.ui.components.CameraToolButtons
import com.example.photogether.ui.components.FocalPill
import com.example.photogether.ui.components.ExposureReadout
import com.example.photogether.ui.components.ExposureReadoutValues
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

/** Width of the landscape left rail; the white balance and ratio cards open just to its right. */
private val RAIL_WIDTH = 60.dp

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
    // The strip is the original, then the chosen camera's filters. The recipes camera also lists every saved recipe.
    val stripIds = CameraProfiles.stripIds(cameraProfile)
    val sources = remember(allSources, recipes, stripIds, cameraProfile) {
        val own = stripIds.mapNotNull { id -> allSources.firstOrNull { it.id == id } }
        own + if (cameraProfile?.showsRecipes == true) recipes.map { RecipeFilters.toFilterInfo(it) } else emptyList()
    }
    var flash by remember { mutableStateOf<NameFlash?>(null) }
    val adjust by cameraViewModel.adjust.collectAsStateWithLifecycle()
    val measuredKelvin by cameraViewModel.measuredKelvin.collectAsStateWithLifecycle()
    val measuredExposure by cameraViewModel.measuredExposure.collectAsStateWithLifecycle()
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
    val resources = LocalResources.current
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
    // Back from another screen: the frame frozen for the screen change is dropped once the return transition ends.
    var resumeTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumeTick++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(resumeTick) {
        if (frozenFrame != null) {
            delay(320)
            frozenFrame = null
        }
    }
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
    // Landscape: the white balance card beside the plate. CameraAdjustPanel reads openTool, so it follows this.
    var wbOpen by remember { mutableStateOf(false) }
    // Vertical centres (root px) of the two rail buttons that open a card, so the card opens beside the one pressed.
    var wbAnchorY by remember { mutableFloatStateOf(0f) }
    var aspectAnchorY by remember { mutableFloatStateOf(0f) }
    val setWbOpen: (Boolean) -> Unit = { open ->
        wbOpen = open
        if (open) panel = CameraPanel.NONE
        val wanted = if (open) CameraTool.WHITE_BALANCE else null
        if (adjust.openTool != wanted) {
            adjust.openTool?.let { cameraViewModel.toggleTool(it) }
            wanted?.let { cameraViewModel.toggleTool(it) }
        }
    }
    // The filter fan, open while the filter button is held.
    var fan by remember { mutableStateOf<FanUiState?>(null) }
    val flashMode by cameraViewModel.flashMode.collectAsStateWithLifecycle()
    var flashFx by remember { mutableStateOf<FlashFx?>(null) }
    // The gallery iris shows the last photo taken this session. captureSeq counts shutter presses, so the
    // iris closes on each one and reopens on the new photo.
    // rememberSaveable: the screen leaves composition when the gallery opens, and a plain remember would reset the box.
    var lastPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var captureSeq by remember { mutableIntStateOf(0) }
    // Guided tour after onboarding. Each step moves on when the user does what its card asks.
    val tutorial = koinViewModel<TutorialViewModel>()
    val tutorialStep by tutorial.step.collectAsStateWithLifecycle()

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
    // A pick on the fan. Kept apart from onSelect, which also runs when a new camera applies its first filter,
    // so that automatic pick does not skip the tour's filter step.
    val onPickFilter: (String) -> Unit = { id ->
        onSelect(id)
        tutorial.advance(TutorialStep.LOOK)
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
    // The grid is set on the Settings screen. Read it on resume so a change made there applies.
    var showGrid by remember { mutableStateOf(GridPrefs.read(context)) }
    // The exposure readout is set on the Settings screen too, and read on resume the same way.
    var showExposure by remember { mutableStateOf(ExposureOverlayPrefs.read(context)) }
    // The round wheel choice is set on the Settings screen too, and read on resume the same way.
    var roundWheel by remember { mutableStateOf(RoundWheelPrefs.read(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraViewModel.reloadShutterSound()
                cameraViewModel.reloadCropZoom()
                showGrid = GridPrefs.read(context)
                showExposure = ExposureOverlayPrefs.read(context)
                roundWheel = RoundWheelPrefs.read(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A camera saved by the picker (from Home or from here) applies its first filter, unless the current
    // filter already belongs to it.
    LaunchedEffect(savedCameraId) {
        val picked = savedCameraId?.let { CameraProfiles.byId(it) } ?: return@LaunchedEffect
        if (!picked.offers(selectedId)) currentOnSelect(picked.filterId)
    }

    // Orientation is owned by AppNavigation (camera + picker follow the sensor, the rest is portrait).
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Turning from landscape to portrait hands exposure back to the camera (mode A). The previous orientation is
    // saved, because a rotation recreates the activity, so only the transition triggers it and a first open in portrait does not.
    var wasLandscape by rememberSaveable { mutableStateOf(landscape) }
    LaunchedEffect(landscape) {
        if (wasLandscape && !landscape) cameraViewModel.setManualMode(false)
        wasLandscape = landscape
    }
    // First-run tip on the portrait camera. Dismissed by a tap or by rotating, and then never shown again.
    var showTip by remember { mutableStateOf(RotateTipPrefs.shouldShow(context)) }
    val dismissTip: () -> Unit = {
        showTip = false
        RotateTipPrefs.markSeen(context)
    }
    LaunchedEffect(landscape) {
        if (landscape && showTip) dismissTip()
    }
    val viewfinderLayers: @Composable BoxScope.() -> Unit = {
            CameraPreviewViewfinder(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize(),
                previewView = previewView,
                previewUseCase = cameraViewModel.preview.value,
                camera = camera,
                flash = flash,
                showGrid = showGrid,
                exposure = if (showExposure) exposureReadoutValues(adjust, measuredExposure) else null,
                aspect = effectiveAspect,
                cropZoom = cameraViewModel.cropZoom.floatValue,
                cropZoomOn = cameraViewModel.cropZoomActive.value,
                zoomMm = cropZoomMm,
                onStrength = onStrength,
                onFocusPoint = cameraViewModel::focusAt,
                onPinch = cameraViewModel::pinchZoom,
                onPinchEnd = cameraViewModel::endPinch,
                        landscape = landscape,
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
    }
    val quickBar: @Composable () -> Unit = {
        CameraQuickBar(
            modifier = Modifier.tutorialTarget("quickbar"),
            kelvin = adjust.kelvin ?: measuredKelvin,
            kelvinMeasured = adjust.kelvin == null,
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
    val panelBody: @Composable (CameraPanel) -> Unit = { shown ->
        when (shown) {
            CameraPanel.NONE -> Unit
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
                    text = (adjust.kelvin?.let { kelvinLabel(it) }
                        ?: measuredKelvin?.let { "AUTO · ~${it}K" }
                        ?: kelvinLabel(null)).uppercase(),
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
                        measuredKelvin = measuredKelvin,
                        onKelvinChange = { cameraViewModel.setKelvin(it) },
                        onKelvinAuto = cameraViewModel::setKelvinAuto,
                        onEvChange = cameraViewModel::setEv,
                        onTintChange = cameraViewModel::setTint,
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
                        onTintChange = cameraViewModel::setTint,
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
    val onCapture: () -> Unit = {
            // Flash and iris only play when the press is accepted (a capture in progress ignores it).
            val accepted = cameraViewModel.takePhotoToGallery(
                cameraId = cameraProfile?.id ?: FilterCatalog.ORIGINAL_ID,
                onSaved = { uri -> lastPhoto = uri },
                onError = { exception ->
                    Log.e(TAG, "Capture failed", exception)
                    Toast.makeText(context, resources.getString(R.string.camera_save_failed), Toast.LENGTH_SHORT).show()
                },
            )
            if (accepted) {
                if (flashMode == FlashMode.ON && cameraViewModel.hasFlash.value) {
                    flashFx = FlashFx(FlashStyle.HARSH)
                }
                captureSeq++
                tutorial.advance(TutorialStep.CAPTURE)
                tutorial.advance(TutorialStep.SHOOT_MORE)
            }
    }

    // Volume keys shoot only in the landscape body, which is the mode with the physical-style controls.
    val currentOnCapture by rememberUpdatedState(onCapture)
    DisposableEffect(landscape) {
        if (landscape) VolumeKeyCapture.handler = { currentOnCapture() }
        onDispose { VolumeKeyCapture.handler = null }
    }

    TutorialLayer(
        step = tutorialStep,
        screen = TutorialScreen.CAMERA,
        onNext = { tutorialStep?.let(tutorial::advance) },
        onSkip = tutorial::skip,
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (landscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(InkColors.background)
                .safeDrawingPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left rail: back, settings, flash, aspect and camera switch, like the buttons on a body's left.
            Column(
                modifier = Modifier.fillMaxHeight().width(RAIL_WIDTH),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(onClick = { navController.popBackStackOnce() }) {
                    Icon(AppIcons.Back, stringResource(R.string.action_back), tint = InkColors.textPrimary)
                }
                IconButton(onClick = { navController.navigate(AppRoutes.SETTINGS) }) {
                    Icon(AppIcons.Menu, stringResource(R.string.settings_title), tint = InkColors.textPrimary)
                }
                val flashAvailable = cameraViewModel.hasFlash.value
                CameraCircleButton(
                    icon = flashIcon(flashMode),
                    label = stringResource(R.string.camera_flash_fmt, flashLabel(flashMode)),
                    onClick = { if (flashAvailable) cameraViewModel.setFlashMode(flashMode.next()) },
                    buttonSize = CAMERA_BUTTON_SIZE,
                    contentColor = if (flashAvailable) InkColors.textPrimary else InkColors.textSecondary,
                )
                Box(modifier = Modifier.onGloballyPositioned { aspectAnchorY = it.positionInRoot().y + it.size.height / 2f }) {
                    AspectButton(label = effectiveAspect.label, onClick = { openPanel(CameraPanel.ASPECT) })
                }
                Box(
                    modifier = Modifier
                        .tutorialTarget("quickbar")
                        .onGloballyPositioned { wbAnchorY = it.positionInRoot().y + it.size.height / 2f },
                ) {
                    CameraCircleButton(
                        icon = AppIcons.Temperature,
                        label = "Cân bằng trắng",
                        onClick = { setWbOpen(!wbOpen) },
                        buttonSize = CAMERA_BUTTON_SIZE,
                        containerColor = if (wbOpen) InkColors.accent else InkColors.surfaceElevated.copy(alpha = 0.7f),
                        contentColor = if (wbOpen) InkColors.onAccent else InkColors.textPrimary,
                    )
                }
                CameraCircleButton(
                    icon = AppIcons.SwitchCamera,
                    label = stringResource(R.string.camera_switch),
                    onClick = cameraViewModel::switchFacing,
                    buttonSize = CAMERA_BUTTON_SIZE,
                )
            }
            // Viewfinder in landscape: the same crop as the capture, so the ratio is the portrait one turned on its side.
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.aspectRatio(1f / viewRatio)) {
                    viewfinderLayers()
                }
            }
            CameraBodyPlate(
                adjust = adjust,
                roundWheel = roundWheel,
                viewRatio = viewRatio,
                measured = measuredExposure,
                onManualMode = cameraViewModel::setManualMode,
                onIso = cameraViewModel::setManualIso,
                onShutter = cameraViewModel::setManualShutter,
                onEv = { ev ->
                    cameraViewModel.setEv(ev)
                    cameraViewModel.commitAdjust()
                },
                onCapture = onCapture,
                onOpenGallery = {
                    tutorial.advance(TutorialStep.OPEN_GALLERY)
                    navController.navigate(AppRoutes.CAMERA_GALLERY)
                },
                onOpenPicker = { navController.navigateOnce(AppRoutes.CAMERA_PICKER) },
                pickerName = cameraProfile?.name,
                pickerCameraId = cameraProfile?.id,
                lastPhoto = lastPhoto,
                captureSeq = captureSeq,
                filters = sources,
                onSelectFilter = onPickFilter,
                onFan = { fan = it },
            )
        }
        // Card that opens right beside the rail button that was pressed (white balance or ratio), growing out of it.
        // A clickable scrim closes it.
        val cardOpen = wbOpen || panel == CameraPanel.ASPECT
        // Keeps the last kind while the card animates out, so its content does not swap mid-exit.
        var lastCardWb by remember { mutableStateOf(true) }
        SideEffect { if (cardOpen) lastCardWb = wbOpen }
        val cardWb = if (cardOpen) wbOpen else lastCardWb
        val cardAnchorY by animateFloatAsState(
            targetValue = if (cardWb) wbAnchorY else aspectAnchorY,
            animationSpec = tween(220),
            label = "cardAnchor",
        )
        var overlayTop by remember { mutableFloatStateOf(0f) }
        val dismiss: () -> Unit = {
            if (wbOpen) setWbOpen(false) else openPanel(CameraPanel.ASPECT)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .onGloballyPositioned { overlayTop = it.positionInRoot().y },
        ) {
            if (cardOpen) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismiss,
                    ),
                )
            }
            AnimatedVisibility(
                visible = cardOpen,
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val x = RAIL_WIDTH.roundToPx() + Spacing.sm.roundToPx()
                    // Centred on the button, kept inside the window.
                    val y = (cardAnchorY - overlayTop - placeable.height / 2f).roundToInt()
                        .coerceIn(0, (constraints.maxHeight - placeable.height).coerceAtLeast(0))
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x, y) }
                },
                enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.85f, transformOrigin = TransformOrigin(0f, 0.5f)),
                exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.85f, transformOrigin = TransformOrigin(0f, 0.5f)),
            ) {
                Box(
                    modifier = Modifier
                        .width(if (cardWb) 320.dp else 260.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        // Swallow taps on the card so they do not reach the scrim.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                        .padding(Spacing.md),
                ) {
                    if (cardWb) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = (adjust.kelvin?.let { kelvinLabel(it) }
                                    ?: measuredKelvin?.let { "AUTO · ~${it}K" }
                                    ?: kelvinLabel(null)).uppercase(),
                                style = BinsooType.label,
                                color = InkColors.textPrimary,
                            )
                            CameraAdjustPanel(
                                state = adjust,
                                measuredKelvin = measuredKelvin,
                                onKelvinChange = { cameraViewModel.setKelvin(it) },
                                onKelvinAuto = cameraViewModel::setKelvinAuto,
                                onEvChange = cameraViewModel::setEv,
                                onTintChange = cameraViewModel::setTint,
                                onCommit = cameraViewModel::commitAdjust,
                                modifier = Modifier.fillMaxWidth(),
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
                    } else {
                        panelBody(CameraPanel.ASPECT)
                    }
                }
            }
        }
      } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(InkColors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            CameraTopBar(
                onBack = { navController.popBackStackOnce() },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
            )

            // The viewfinder takes the space the rest of the screen leaves. Its bottom overlays sit inside it.
            // Its shape follows the chosen aspect, so the live view shows the same crop as the capture.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                // Centred so the frame has the same space above and below it.
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.aspectRatio(viewRatio)) {
                    viewfinderLayers()
                    // Overlays the bottom edge of the viewfinder. Kept outside the viewfinder's swipe box so
                    // slider drags do not also cycle filters.
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        quickBar()
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
                // The panel grows out of the pill that opened it, like liquid glass pouring from its entry, and
                // shrinks back into it on close. Entries sit left (WB), centre (lens) and right (EV) of the bar above.
                AnimatedContent(
                    targetState = panel,
                    transitionSpec = {
                        val spring = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                        val enter = (fadeIn(tween(160)) +
                            scaleIn(spring, initialScale = 0.35f, transformOrigin = panelOrigin(targetState)) +
                            slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { -it / 3 })
                        val exit = (fadeOut(tween(110)) +
                            scaleOut(tween(160), targetScale = 0.5f, transformOrigin = panelOrigin(initialState)) +
                            slideOutVertically(tween(160)) { -it / 4 })
                        enter.togetherWith(exit).using(SizeTransform(clip = false))
                    },
                    label = "cameraPanel",
                ) { shown ->
                when (shown) {
                    CameraPanel.NONE -> Row(
                        // Top of the slot, right under the frame; the panels below keep their centred layout.
                        modifier = Modifier.align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
                    ) {
                        // Flash is off on the front camera, so its button is dimmed and does nothing there.
                        val flashAvailable = cameraViewModel.hasFlash.value
                        CameraCircleButton(
                            icon = flashIcon(flashMode),
                            label = stringResource(R.string.camera_flash_fmt, flashLabel(flashMode)),
                            onClick = { if (flashAvailable) cameraViewModel.setFlashMode(flashMode.next()) },
                            buttonSize = CAMERA_BUTTON_SIZE,
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
                            buttonSize = CAMERA_BUTTON_SIZE,
                        )
                    }
                    else -> panelBody(shown)
                }
                }
            }

            // Press and hold to open the filter fan upward over the viewfinder; it replaces the old horizontal strip.
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                FilterFanButton(
                    filters = sources,
                    onSelect = onPickFilter,
                    onFan = { fan = it },
                    preferredMidDeg = 270f,
                    modifier = Modifier.tutorialTarget("filter"),
                    pillName = sources.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.camera_filter_fallback),
                )
            }

            ManualControls(
                onClickCapture = {
                    onCapture()
                },
                onOpenGallery = {
                    tutorial.advance(TutorialStep.OPEN_GALLERY)
                    navController.navigate(AppRoutes.CAMERA_GALLERY)
                },
                onOpenPicker = {
                    // Freeze the last frame first, so the live preview cannot stutter while the screen changes.
                    navController.navigateOnce(AppRoutes.CAMERA_PICKER)
                },
                pickerName = cameraProfile?.name,
                pickerCameraId = cameraProfile?.id,
                lastPhoto = lastPhoto,
                captureSeq = captureSeq,
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        }
      }
      // The filter fan, open while the filter button (landscape plate or portrait pill) is held.
      fan?.let { FilterFanOverlay(state = it, filters = sources, selectedId = selectedId) }
      // Held back during the tour, so the two cards do not stack.
      if (!landscape && showTip && tutorialStep == null) {
          RotateTip(
              onDismiss = dismissTip,
              modifier = Modifier.align(Alignment.Center),
          )
      }
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
 * The rounded viewfinder (28dp corners): the preview, the filter-name flash, the grid, and the focus ring. The zoom and tool pills are drawn by [CameraScreen] over its bottom edge.
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
    showGrid: Boolean = false,
    exposure: ExposureReadoutValues? = null,
    aspect: CameraAspect = CameraAspect.FOUR_THREE,
    cropZoom: Float = 1f,
    cropZoomOn: Boolean = false,
    zoomMm: Int = 0,
    onStrength: (delta: Float) -> Unit,
    onFocusPoint: (MeteringPoint) -> Unit = {},
    onPinch: (factor: Float) -> Unit = {},
    onPinchEnd: () -> Unit = {},
    landscape: Boolean = false,
) {
    val currentOnStrength by rememberUpdatedState(onStrength)
    val currentOnFocusPoint by rememberUpdatedState(onFocusPoint)
    val currentOnPinch by rememberUpdatedState(onPinch)
    val currentOnPinchEnd by rememberUpdatedState(onPinchEnd)
    // Last tap position in the viewfinder, and a counter that replays the focus ring on each tap.
    var focusPos by remember { mutableStateOf<Offset?>(null) }
    var focusTrigger by remember { mutableIntStateOf(0) }

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
            if (aspect.crops && !cropZoomOn) AspectMask(aspect.widthOverHeight, landscape)
            // At 1x the frame would only outline the whole view, so it shows only once the crop is zoomed in.
            if (cropZoomOn && cropZoom > 1f) CropZoomFrame(aspect.widthOverHeight.takeIf { aspect.crops }, cropZoom, "${zoomMm}mm", landscape)
            if (showGrid) GridOverlay()
            exposure?.let { ExposureReadout(values = it, modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.sm)) }
            FocusIndicator(position = focusPos, trigger = focusTrigger)
        }
    }
}

/**
 * Values for the live exposure readout. In manual mode they are the ones the wheels hold (EV does not apply).
 * In auto they are what the sensor used for the latest frame, snapped to the nearest standard step, and EV.
 * Before the first frame the auto values show as "--".
 */
private fun exposureReadoutValues(adjust: CameraAdjustState, measured: MeasuredExposure?): ExposureReadoutValues {
    if (adjust.manualExposureActive && adjust.manualAvailable) {
        return ExposureReadoutValues(
            tv = ManualExposureSteps.shutterLabel(adjust.manualExposureNs),
            iso = ManualExposureSteps.isoLabel(adjust.manualIso),
            ev = null,
        )
    }
    val ev = formatEv(adjust.ev)
    if (measured == null) return ExposureReadoutValues(tv = "--", iso = "--", ev = ev)
    val shutterSteps = adjust.exposureRangeNs?.let { ManualExposureSteps.shutterSteps(it) }.orEmpty()
    val isoSteps = adjust.isoRange?.let { ManualExposureSteps.isoSteps(it) }.orEmpty()
    val exposureNs = if (shutterSteps.isEmpty()) measured.exposureNs
        else shutterSteps[ManualExposureSteps.nearestIndex(shutterSteps, measured.exposureNs)]
    val iso = if (isoSteps.isEmpty()) measured.iso
        else isoSteps[ManualExposureSteps.nearestIndex(isoSteps, measured.iso)]
    return ExposureReadoutValues(
        tv = ManualExposureSteps.shutterLabel(exposureNs),
        iso = ManualExposureSteps.isoLabel(iso),
        ev = ev,
    )
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
    ManualControls(onClickCapture = {})
}
// endregion

/**
 * Darkens the viewfinder outside the saved crop. The crop is placed with the same FILL_CENTER rule PreviewView
 * uses for the 3:4 camera frame ([CropMath.visibleCrop]), so the mask matches the capture on any viewfinder size.
 */
@Composable
private fun AspectMask(ratio: Float, landscape: Boolean = false) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val r = CropMath.visibleCrop(w, h, turn(CameraAspect.FOUR_THREE.widthOverHeight, landscape), turn(ratio, landscape))
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
private fun CropZoomFrame(aspectRatio: Float?, cropZoom: Float, mmLabel: String, landscape: Boolean = false) {
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
        val ratio = turn(aspectRatio ?: CameraAspect.FOUR_THREE.widthOverHeight, landscape)
        // Zoom the unclipped crop (what the capture keeps), then clip to the view.
        val r = CropMath.clipTo(
            CropMath.zoomCrop(CropMath.unclippedCrop(w, h, turn(CameraAspect.FOUR_THREE.widthOverHeight, landscape), ratio), cropZoom),
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

/** Where a panel grows from: the horizontal position, across the width, of the control that opens it. */
private fun panelOrigin(panel: CameraPanel): TransformOrigin = when (panel) {
    CameraPanel.WB -> TransformOrigin(0.3f, 0f)
    CameraPanel.EV -> TransformOrigin(0.7f, 0f)
    else -> TransformOrigin(0.5f, 0f)
}

/** A portrait width/height ratio as seen with the phone on its side. */
private fun turn(ratio: Float, landscape: Boolean): Float = if (landscape) 1f / ratio else ratio
