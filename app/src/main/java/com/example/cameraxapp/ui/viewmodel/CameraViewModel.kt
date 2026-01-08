package com.example.cameraxapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.service.CameraService
import com.example.cameraxapp.service.ImageProcessingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val imageProcessingService = ImageProcessingService()
    private val cameraService = CameraService()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow() // use for view

    private val _countdown = MutableStateFlow(3)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    var imageCapture = mutableStateOf<ImageCapture?>(null)
    var preview = mutableStateOf<Preview?>(null)
    val executor = ContextCompat.getMainExecutor(application)

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

    fun takePhoto(
        onPhotoCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) = viewModelScope.launch(Dispatchers.Default) {
        if (isTakingPicture) {
            Log.d("CameraViewModel", "Skipping photo capture - too soon or already taking picture")
            return@launch
        }
        isTakingPicture = true

        try {
            val photoFile = imageProcessingService.createPhotoFile(getApplication())
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.value?.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                        if (photoFile.exists()) {
                            val croppedUri = imageProcessingService.saveCroppedImage(
                                getApplication(),
                                savedUri,
                                photoFile
                            )
                            onPhotoCaptured(croppedUri)
//                            onPhotoCaptured(savedUri)
                        } else {
                            Log.e(
                                "CameraViewModel",
                                "Photo file does not exist: ${photoFile.absolutePath}"
                            )
                            onError(
                                ImageCaptureException(
                                    ImageCapture.ERROR_FILE_IO,
                                    "Failed to save photo",
                                    null
                                )
                            )
                        }
                        isTakingPicture = false
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(
                            "CameraViewModel",
                            "Error capturing photo: ${exception.message}",
                            exception
                        )
                        isTakingPicture = false
                        onError(exception)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error in takePhoto: ${e.message}", e)
            isTakingPicture = false
            onError(
                ImageCaptureException(
                    ImageCapture.ERROR_UNKNOWN,
                    e.message ?: "Unknown error",
                    e
                )
            )
        }
    }

    fun initCamera(lifecycleOwner: LifecycleOwner) = viewModelScope.launch(Dispatchers.Default) {
        try {

            withContext(Dispatchers.Main) {
                cameraService.bindCameraUseCases(
                    context = getApplication(),
                    lifecycleOwner = lifecycleOwner,
                )
            }

            preview.value = cameraService.preview
            imageCapture.value = cameraService.imageCapture
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error setting up camera: ${e.message}", e)
            preview.value = null
            imageCapture.value = null
        }
    }
}
