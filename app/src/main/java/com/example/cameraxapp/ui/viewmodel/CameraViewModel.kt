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
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.FilterCatalog
import com.example.cameraxapp.service.CameraService
import com.example.cameraxapp.service.ImageProcessingService
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Screen-facing state. All camera binding and capture goes through [CameraService]. */
class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val imageProcessingService = ImageProcessingService()
    val catalog = FilterCatalog(application)
    private val cameraService = CameraService(catalog)
    private val executor = ContextCompat.getMainExecutor(application)

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow() // use for view

    private val _countdown = MutableStateFlow(3)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    val preview = mutableStateOf<Preview?>(null)
    val camera = mutableStateOf<Camera?>(null)

    private var isTakingPicture = false // use for view model

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
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera: ${e.message}", e)
            preview.value = null
            camera.value = null
        }
    }

    /** Sets the live and captured filter. Does not rebind the camera. */
    fun setFilter(params: EditParams) {
        cameraService.setFilter(params)
    }

    /** Manual exposure (ns) and ISO. Applied to the bound camera only when the user changes them. */
    fun setManualExposure(exposureNs: Long, iso: Int) {
        camera.value?.let { cameraService.setManualExposure(it, exposureNs, iso) }
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
                            .onSuccess(onResult)
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
