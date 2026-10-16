package com.example.cameraxapp.ui.viewmodel

import android.app.Application
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
import androidx.camera.core.Preview
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.core.camera.CameraAdjustState
import com.example.cameraxapp.core.camera.CameraTool
import com.example.cameraxapp.core.camera.EvMode
import com.example.cameraxapp.core.camera.withCameraAdjust
import com.example.cameraxapp.core.camera.withCapabilities
import com.example.cameraxapp.core.camera.withEv
import com.example.cameraxapp.core.camera.withKelvin
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.FilterCatalog
import com.example.cameraxapp.core.render.Recipe
import com.example.cameraxapp.data.repository.RecipeRepository
import com.example.cameraxapp.data.settings.CameraSettings
import com.example.cameraxapp.data.settings.CameraSettingsRepository
import com.example.cameraxapp.service.CameraService
import com.example.cameraxapp.core.analytics.Analytics
import com.example.cameraxapp.core.analytics.AnalyticsEvent
import com.example.cameraxapp.core.analytics.NoOpAnalytics
import com.example.cameraxapp.service.ImageProcessingService
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Screen-facing state. All camera binding and capture goes through [CameraService]. */
class CameraViewModel(
    application: Application,
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: CameraSettingsRepository,
    private val analytics: Analytics = NoOpAnalytics,
) : AndroidViewModel(application) {
    private val imageProcessingService = ImageProcessingService()
    val catalog = FilterCatalog(application)

    /** Saved recipes, newest first, for the filter strip. */
    val recipes: Flow<List<Recipe>> = recipeRepository.observeRecipes()
    private val cameraService = CameraService(catalog)
    private val executor = ContextCompat.getMainExecutor(application)

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow() // use for view

    private val _countdown = MutableStateFlow(3)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    val preview = mutableStateOf<Preview?>(null)
    val camera = mutableStateOf<Camera?>(null)
    val editParams = mutableStateOf(EditParams())

    private val _adjust = MutableStateFlow(CameraAdjustState())

    /** Kelvin, EV and open panel. The filter edit itself stays in [editParams]. */
    val adjust: StateFlow<CameraAdjustState> = _adjust.asStateFlow()

    /** Restores the saved Kelvin and EV once. [initCamera] waits for it before applying hardware settings. */
    private val restoreJob: Job

    private var isTakingPicture = false // use for view model

    init {
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

    /** Binds the camera once for [lifecycleOwner]. Preview and camera state are exposed for the view. */
    fun initCamera(
        lifecycleOwner: LifecycleOwner,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            val bound = cameraService.bind(getApplication(), lifecycleOwner, lensFacing)
            preview.value = bound.preview
            camera.value = bound.camera
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

    /** Sets the live and captured filter. Does not rebind the camera. */
    fun setFilter(params: EditParams) {
        editParams.value = params
        pushRender()
    }

    /** Selects a catalog filter by id, keeping the other edit values. */
    fun selectFilter(filterId: String) {
        setFilter(editParams.value.copy(filterId = filterId))
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

    /** Captures a filtered JPEG and copies it into the gallery under Pictures/CameraX-Image. */
    fun takePhotoToGallery(
        onSaved: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        capture(onError, onResult = onSaved) { file ->
            copyToGallery(file).also { file.delete() }
        }
    }

    private fun capture(
        onError: (ImageCaptureException) -> Unit,
        onResult: (Uri) -> Unit,
        transform: (File) -> Uri,
    ) {
        if (isTakingPicture) {
            Log.d(TAG, "Skipping photo capture - already taking picture")
            return
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
            cameraService.takePhoto(
                file = photoFile,
                executor = executor,
                onSaved = { saved ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val result = runCatching { withContext(Dispatchers.IO) { transform(saved) } }
                        isTakingPicture = false
                        result
                            .onSuccess {
                                analytics.log(AnalyticsEvent.PHOTO_CAPTURED)
                                onResult(it)
                            }
                            .onFailure { e ->
                                Log.e(TAG, "Saving photo failed: ${e.message}", e)
                                onError(ImageCaptureException(ImageCapture.ERROR_FILE_IO, "Failed to save photo", e))
                            }
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error capturing photo: ${exception.message}", exception)
                    isTakingPicture = false
                    onError(exception)
                },
            )
        }
    }

    private fun copyToGallery(file: File): Uri {
        val app: Context = getApplication()
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        val uri = app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Cannot create gallery entry")
        app.contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: throw IOException("Cannot open gallery entry")
        return uri
    }

    override fun onCleared() {
        cameraService.shutdown()
        super.onCleared()
    }

    private companion object {
        const val TAG = "CameraViewModel"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
