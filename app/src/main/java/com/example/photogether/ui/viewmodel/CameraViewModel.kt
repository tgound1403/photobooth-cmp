package com.example.photogether.ui.viewmodel

import android.app.Application
import android.media.AudioManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.photogether.core.camera.CameraAdjustState
import com.example.photogether.core.camera.CameraTool
import com.example.photogether.core.colors.PendingColorLook
import com.example.photogether.core.camera.CropZoomPrefs
import com.example.photogether.core.camera.Lens
import com.example.photogether.core.camera.ShutterSoundPlayer
import com.example.photogether.core.camera.ShutterSoundPrefs
import com.example.photogether.core.camera.EvMode
import com.example.photogether.core.camera.pickLens
import com.example.photogether.core.camera.withCameraAdjust
import com.example.photogether.core.camera.withCapabilities
import com.example.photogether.core.camera.withEv
import com.example.photogether.core.camera.withKelvin
import com.example.photogether.core.camera.zoomFor
import com.example.photogether.core.moments.MomentQuery
import com.example.photogether.core.moments.ShotSettings
import com.example.photogether.core.render.EditParams
import com.example.photogether.core.render.CameraAspect
import com.example.photogether.core.render.FlashMode
import com.example.photogether.core.render.FilterCatalog
import com.example.photogether.core.render.Recipe
import com.example.photogether.core.render.ShutterSound
import com.example.photogether.data.repository.RecipeRepository
import com.example.photogether.data.settings.CameraSettings
import com.example.photogether.data.settings.CameraSettingsRepository
import com.example.photogether.service.CameraService
import com.example.photogether.core.analytics.Analytics
import com.example.photogether.core.analytics.AnalyticsEvent
import com.example.photogether.core.analytics.NoOpAnalytics
import com.example.photogether.service.ImageProcessingService
import com.example.photogether.service.ShotSettingsStore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Screen-facing state. All camera binding and capture goes through [CameraService]. */
class CameraViewModel(
    application: Application,
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: CameraSettingsRepository,
    private val analytics: Analytics = NoOpAnalytics,
    private val pendingLook: PendingColorLook = PendingColorLook(),
    val catalog: FilterCatalog,
) : AndroidViewModel(application) {
    private val imageProcessingService = ImageProcessingService()

    /** Saved recipes, newest first, for the filter strip. */
    val recipes: Flow<List<Recipe>> = recipeRepository.observeRecipes()
    private val cameraService = CameraService(catalog)
    private val executor = ContextCompat.getMainExecutor(application)

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow() // use for view

    private val _countdown = MutableStateFlow(3)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.OFF)

    /** Current flash mode. A new camera body resets it to its default. */
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    private val _shutterSoundEnabled = MutableStateFlow(ShutterSoundPrefs.read(application))

    /** Whether the shutter sound plays on capture (still subject to the ringer mode). Remembered across launches. */
    val shutterSoundEnabled: StateFlow<Boolean> = _shutterSoundEnabled.asStateFlow()

    // Created on the first shot, so the sound pool and its asset reads stay off the first frame.
    private var shutterSoundPlayer: ShutterSoundPlayer? = null
    private val audioManager = application.getSystemService(AudioManager::class.java)

    /** Shutter sound every capture plays. Cameras no longer choose one. */
    private val cameraShutterSound = ShutterSound.MECH

    /** True when the bound camera has a flash unit. */
    val hasFlash = mutableStateOf(false)

    val preview = mutableStateOf<Preview?>(null)
    val camera = mutableStateOf<Camera?>(null)
    val editParams = mutableStateOf(EditParams())

    private val _adjust = MutableStateFlow(CameraAdjustState())

    /** Kelvin, EV and open panel. The filter edit itself stays in [editParams]. */
    val adjust: StateFlow<CameraAdjustState> = _adjust.asStateFlow()

    /** The camera body last picked (DataStore). Null until one is picked. */
    val savedCameraId: Flow<String?> = settingsRepository.settings.map { it.cameraId }

    /** Restores the saved Kelvin and EV once. [initCamera] waits for it before applying hardware settings. */
    private val restoreJob: Job

    private var isTakingPicture = false // use for view model

    init {
        // A colour story's "Try now" hands its look over here; it replaces the default filter once.
        pendingLook.take()?.let { editParams.value = it }
        restoreJob = viewModelScope.launch {
            val saved = try {
                settingsRepository.settings.first()
            } catch (e: IOException) {
                Log.w(TAG, "Cannot read camera settings, using defaults: ${e.message}", e)
                CameraSettings()
            }
            _adjust.update { it.withKelvin(saved.kelvin).withEv(saved.ev) }
            pushRender()
        }
    }

    fun startCapturing() {
        _isCapturing.value = true
    }

    fun stopCapturing() {
        _isCapturing.value = false
        isTakingPicture = false
    }

    fun updateCountdown(value: Int) {
        _countdown.value = value
    }

    /** 35mm-equivalent focal length of the lens in use. The zoom pill multiplies it by the zoom ratio. */
    val lensMm = mutableFloatStateOf(35f)

    /** Physical or digital lenses the focal panel offers, sorted by focal length and distinct by rounded mm. */
    val availableLenses = mutableStateOf<List<Lens>>(emptyList())

    /** 35mm-equivalent focal length of the main lens, chosen on the first bind. The focal panel shows ratios against it. */
    val mainMm = mutableFloatStateOf(35f)

    /** True once [mainMm] has been set, so later lens switches do not change the main lens. */
    private var mainMmChosen = false

    /** Aspect picked on the camera screen. Null means the camera body's own aspect. Reset when the camera changes. */
    val aspectOverride = mutableStateOf<CameraAspect?>(null)

    /** True when zoom shows a crop frame on the preview instead of magnifying it. Read from [CropZoomPrefs]. */
    private var cropZoomEnabled = CropZoomPrefs.read(application)

    /** Extra zoom that is applied by cropping the capture, not by the camera. 1 means none. Effective zoom = camera ratio * this. */
    val cropZoom = mutableFloatStateOf(1f)

    /** True while crop-frame zoom is on, so the screen shows the frame at every zoom level. */
    val cropZoomActive = mutableStateOf(cropZoomEnabled)

    private fun updateCropZoom(value: Float) {
        cropZoom.floatValue = value
        cameraService.cropZoom = value
    }

    /** Camera2 id of the lens in use. Null when the device has a single back camera. */
    private var lensId: String? = null
    private var owner: LifecycleOwner? = null
    private var owningFacing = CameraSelector.LENS_FACING_FRONT

    /** Binds the camera once for [lifecycleOwner]. Preview and camera state are exposed for the view. */
    fun initCamera(
        lifecycleOwner: LifecycleOwner,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    ) = viewModelScope.launch(Dispatchers.Main) {
        owner = lifecycleOwner
        owningFacing = lensFacing
        updateCropZoom(1f)
        if (lensFacing == CameraSelector.LENS_FACING_BACK && lensId == null) {
            // On a multi-camera phone, start on the physical lens for 35mm rather than the logical camera.
            val all = cameraService.findBackLenses(getApplication())
            if (all.size > 1) lensId = pickLens(35f, all)?.cameraId
        }
        try {
            val bound = cameraService.bind(getApplication(), lifecycleOwner, lensFacing, lensId)
            preview.value = bound.preview
            camera.value = bound.camera
            hasFlash.value = cameraService.hasFlash(bound.camera)
            cameraService.setFlashMode(bound.camera, _flashMode.value)
            lensMm.floatValue = cameraService.lenses.firstOrNull { it.cameraId == lensId }?.equivMm ?: lensMm.floatValue
            availableLenses.value = cameraService.lenses.sortedBy { it.equivMm }.distinctBy { it.equivMm.roundToInt() }
            if (!mainMmChosen) {
                mainMm.floatValue = lensMm.floatValue
                mainMmChosen = true
            }
            restoreJob.join()
            _adjust.update { it.withCapabilities(cameraService.capabilities(bound.camera)) }
            applyHardware(bound.camera)
            pushRender()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera: ${e.message}", e)
            preview.value = null
            camera.value = null
        }
    }

    /**
     * Shows the camera at [targetMm] (35mm-equivalent). Switches to the physical lens that suits it
     * when that lens differs from the one in use, then sets the digital zoom on top of it.
     */
    fun setFocal(targetMm: Int) = viewModelScope.launch(Dispatchers.Main) {
        val lens = pickLens(targetMm.toFloat(), cameraService.lenses) ?: return@launch
        if (lens.cameraId != lensId && cameraService.lenses.size > 1) {
            lensId = lens.cameraId
            val bindOwner = owner ?: return@launch
            initCamera(bindOwner, owningFacing).join()
            // Without crop-frame zoom the new lens is shown as it is bound, as before.
            if (!cropZoomEnabled) return@launch
        }
        setZoomRatio(zoomFor(targetMm.toFloat(), lens))
    }

    /**
     * Zooms to [ratio] on the lens in use. Normally the camera zooms. With crop-frame zoom the camera stays at its
     * base ratio and the part above it becomes [cropZoom], which the preview frame shows and the capture crops to.
     */
    fun setZoomRatio(ratio: Float) {
        val cam = camera.value ?: return
        val state = cam.cameraInfo.zoomState.value ?: return
        val target = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        if (!cropZoomEnabled) {
            updateCropZoom(1f)
            cam.cameraControl.setZoomRatio(target)
            return
        }
        val base = 1f.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        // Below the base ratio (ultrawide logical cameras) the camera itself zooms out.
        val camRatio = minOf(target, base)
        if (state.zoomRatio != camRatio) cam.cameraControl.setZoomRatio(camRatio)
        updateCropZoom((target / camRatio).coerceAtLeast(1f))
    }

    /** Zoom target of the pinch in progress. Null between pinches. */
    private var pinchTarget: Float? = null

    /**
     * Pinch on the live view. [factor] is the pinch change for one input event. The target is kept here, not read back
     * from the zoom state, which lags behind the fingers and would make the steps compound wrongly.
     */
    fun pinchZoom(factor: Float) {
        val state = camera.value?.cameraInfo?.zoomState?.value ?: return
        val current = pinchTarget ?: (state.zoomRatio * cropZoom.floatValue)
        val target = (current * factor).coerceIn(state.minZoomRatio, state.maxZoomRatio)
        pinchTarget = target
        setZoomRatio(target)
    }

    /** Ends the pinch in progress. The next pinch starts from the zoom in use. */
    fun endPinch() {
        pinchTarget = null
    }

    /**
     * Re-reads the crop-frame zoom setting. When it changed, the current effective zoom is moved between the
     * camera and the crop frame.
     */
    fun reloadCropZoom() {
        val enabled = CropZoomPrefs.read(getApplication())
        if (enabled == cropZoomEnabled) return
        val effective = (camera.value?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f) * cropZoom.floatValue
        cropZoomEnabled = enabled
        cropZoomActive.value = enabled
        setZoomRatio(effective)
    }

    /** Sets the live and captured filter. Does not rebind the camera. */
    fun setFilter(params: EditParams) {
        editParams.value = params
        pushRender()
    }

    /** Selects a catalog filter by id, keeping the other edit values. Strength goes back to full for the new filter. */
    fun selectFilter(filterId: String) {
        setFilter(editParams.value.copy(filterId = filterId, filterIntensity = 1f))
    }

    /** Moves the strength of the current filter by [delta] (fraction of 0..1) and returns the new value. */
    fun adjustFilterIntensity(delta: Float): Float {
        val next = (editParams.value.filterIntensity + delta).coerceIn(0f, 1f)
        setFilter(editParams.value.copy(filterIntensity = next))
        return next
    }

    /** Switches between the back and front camera, rebinding to the same lifecycle owner. */
    fun switchFacing() {
        val lifecycleOwner = owner ?: return
        val next = if (owningFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        // The back lens id does not exist on the front camera, so it is dropped. Back re-picks the 35mm lens.
        lensId = null
        initCamera(lifecycleOwner, next)
    }

    /** Sets the flash mode: capture flash for OFF, AUTO and ON, torch for TORCH. */
    fun setFlashMode(mode: FlashMode) {
        _flashMode.value = mode
        cameraService.setFlashMode(camera.value, mode)
    }

    /** Turns the shutter sound on or off and remembers the choice. */
    fun setShutterSoundEnabled(enabled: Boolean) {
        _shutterSoundEnabled.value = enabled
        ShutterSoundPrefs.write(getApplication(), enabled)
    }

    /** Re-reads the saved shutter sound setting. The Settings screen changes it while this view model stays alive. */
    fun reloadShutterSound() {
        _shutterSoundEnabled.value = ShutterSoundPrefs.read(getApplication())
    }

    /** Sets the aspect that following captures are centre-cropped to. */
    fun setCameraAspect(aspect: CameraAspect) {
        cameraService.cameraAspect = aspect
    }

    /** Picks the aspect the user chose on the camera screen. The preview mask and the capture crop both follow it. */
    fun setAspect(a: CameraAspect) {
        aspectOverride.value = a
        setCameraAspect(a)
    }


    /** Sets the colour temperature in Kelvin, or null for auto white balance. Live; not saved until [commitAdjust]. */
    fun setKelvin(kelvin: Int?) {
        _adjust.update { it.withKelvin(kelvin) }
        camera.value?.let { applyHardware(it) }
        pushRender()
    }

    /** Returns white balance to auto. */
    fun setKelvinAuto() = setKelvin(null)

    /** Sets exposure compensation in stops, snapped to the camera's step. Live; saved on [commitAdjust]. */
    fun setEv(ev: Float) {
        _adjust.update { it.withEv(ev) }
        camera.value?.let { applyHardware(it) }
        pushRender()
    }

    /** Returns exposure compensation to 0 EV. */
    fun resetEv() = setEv(0f)

    /** Opens [tool]'s panel, or closes it if it is already open. */
    fun toggleTool(tool: CameraTool) {
        _adjust.update { it.copy(openTool = if (it.openTool == tool) null else tool) }
    }

    /** Saves the current Kelvin and EV. Call when the user lifts a finger, not on every slider change. */
    fun commitAdjust() {
        val current = _adjust.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setKelvin(current.kelvin)
                settingsRepository.setEv(current.ev)
            } catch (e: IOException) {
                Log.e(TAG, "Saving camera settings failed: ${e.message}", e)
            }
        }
    }

    /** Manual exposure (ns) and ISO. Applied to the bound camera only when the user changes them. */
    fun setManualExposure(exposureNs: Long, iso: Int) {
        val bound = camera.value ?: return
        cameraService.setManualExposure(bound, exposureNs, iso)
        _adjust.update { it.copy(manualExposureActive = true) }
    }

    /** Returns exposure to auto. */
    fun clearManualExposure() {
        camera.value?.let { cameraService.clearManualExposure(it) }
        _adjust.update { it.copy(manualExposureActive = false) }
    }

    /**
     * Focuses (and meters, unless manual exposure is on) at a tapped preview point. Metering is left
     * alone while manual exposure is active, so it does not override the user's exposure.
     */
    fun focusAt(point: MeteringPoint) {
        val bound = camera.value ?: return
        cameraService.focusAt(bound, point, aeToo = !_adjust.value.manualExposureActive)
    }

    /** Sends the edit with the camera-adjust values (Kelvin, software EV) to the renderer. */
    private fun pushRender() {
        cameraService.setFilter(editParams.value.withCameraAdjust(_adjust.value))
    }

    /** Pushes EV (hardware mode only) and the AWB lock for the current Kelvin to [bound]. */
    private fun applyHardware(bound: Camera) {
        val state = _adjust.value
        if (state.evMode == EvMode.HARDWARE) {
            cameraService.setExposureCompensation(bound, state.evRange.indexFor(state.ev))
        }
        cameraService.setAwbLock(bound, locked = state.kelvin != null)
    }

    /** Captures a filtered JPEG, stores it through [ImageProcessingService] and returns its Uri. */
    fun takePhoto(
        onPhotoCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        capture(onError, onResult = onPhotoCaptured) { file ->
            imageProcessingService.saveCroppedImage(getApplication(), Uri.fromFile(file), file)
        }
    }

    /**
     * Captures a filtered JPEG and copies it into the gallery under Pictures/CameraX-Image, named for [cameraId].
     * The settings are read when the button is pressed, so a change made during the capture does not apply.
     * Returns false when a capture is already running and this press was ignored.
     */
    fun takePhotoToGallery(
        cameraId: String,
        onSaved: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ): Boolean {
        val settings = currentShotSettings()
        return capture(onError, onResult = onSaved) { file ->
            try {
                ShotSettingsStore.write(file, settings)
                copyToGallery(file, cameraId)
            } finally {
                file.delete()
            }
        }
    }

    private fun currentShotSettings(): ShotSettings {
        val zoom = camera.value?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
        return ShotSettings(
            lutName = catalog.get(editParams.value.filterId)?.name,
            ev = adjust.value.ev,
            kelvin = adjust.value.kelvin,
            focalMm = (lensMm.floatValue * zoom * cropZoom.floatValue).roundToInt(),
            // The original has no filter to be strong or weak, so its strength is not recorded.
            filterIntensity = editParams.value.filterIntensity.takeIf { editParams.value.filterId != FilterCatalog.ORIGINAL_ID },
        )
    }

    private fun capture(
        onError: (ImageCaptureException) -> Unit,
        onResult: (Uri) -> Unit,
        transform: (File) -> Uri,
    ): Boolean {
        if (isTakingPicture) {
            Log.d(TAG, "Skipping photo capture - already taking picture")
            return false
        }
        isTakingPicture = true
        viewModelScope.launch(Dispatchers.Main) {
            val photoFile = try {
                withContext(Dispatchers.IO) { imageProcessingService.createPhotoFile(getApplication()) }
            } catch (e: Exception) {
                isTakingPicture = false
                onError(ImageCaptureException(ImageCapture.ERROR_FILE_IO, "Cannot create file", e))
                return@launch
            }
            try {
                cameraService.takePhoto(
                    file = photoFile,
                    executor = executor,
                    onSaved = { saved ->
                        viewModelScope.launch(Dispatchers.Main) {
                            val result = runCatching { withContext(Dispatchers.IO) { transform(saved) } }
                            isTakingPicture = false
                            result
                                .onSuccess {
                                    // Shutter sound and analytics only once the photo is really in the gallery.
                                    playShutterSound()
                                    analytics.log(AnalyticsEvent.PHOTO_CAPTURED)
                                    onResult(it)
                                }
                                .onFailure { e ->
                                    Log.e(TAG, "Saving photo failed: ${e.message}", e)
                                    photoFile.delete()
                                    onError(ImageCaptureException(ImageCapture.ERROR_FILE_IO, "Failed to save photo", e))
                                }
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error capturing photo: ${exception.message}", exception)
                        isTakingPicture = false
                        photoFile.delete()
                        onError(exception)
                    },
                )
            } catch (e: Exception) {
                // Synchronous failure (e.g. camera unbound): release the guard so the shutter works again.
                Log.e(TAG, "Capture could not start: ${e.message}", e)
                isTakingPicture = false
                photoFile.delete()
                onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Capture could not start", e))
            }
        }
        return true
    }

    /** Plays the camera's shutter sound once the capture has succeeded. Silent and vibrate modes are skipped. */
    private fun playShutterSound() {
        // Without an audio service we treat the phone as silent rather than guess.
        val ringer = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_SILENT
        val player = shutterSoundPlayer ?: ShutterSoundPlayer(getApplication()).also { shutterSoundPlayer = it }
        player.play(cameraShutterSound, _shutterSoundEnabled.value, ringer)
    }

    private fun copyToGallery(file: File, cameraId: String): Uri {
        val app: Context = getApplication()
        val stamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val displayName = MomentQuery.cameraShotName(cameraId, stamp)
        val isQOrLater = Build.VERSION.SDK_INT > Build.VERSION_CODES.P
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (isQOrLater) {
                put(MediaStore.Images.Media.RELATIVE_PATH, GALLERY_RELATIVE_PATH)
                // Hidden from the gallery until the bytes are fully written (cleared below).
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                // Pre-Q has no RELATIVE_PATH; the target file path is what MediaStore needs to store the bytes.
                val dir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                    GALLERY_FOLDER,
                ).apply { mkdirs() }
                put(MediaStore.MediaColumns.DATA, File(dir, displayName).absolutePath)
            }
        }
        val resolver = app.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Cannot create gallery entry")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: throw IOException("Cannot open gallery entry")
            if (isQOrLater) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
            return uri
        } catch (e: Exception) {
            // Do not leave an empty or half-written row behind in the gallery.
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    override fun onCleared() {
        cameraService.shutdown()
        shutterSoundPlayer?.release()
        super.onCleared()
    }

    private companion object {
        const val TAG = "CameraViewModel"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val GALLERY_FOLDER = "CameraX-Image"
        const val GALLERY_RELATIVE_PATH = "Pictures/CameraX-Image"
    }
}
