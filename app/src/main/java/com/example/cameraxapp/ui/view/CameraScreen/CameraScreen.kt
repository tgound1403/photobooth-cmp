package com.example.cameraxapp.ui.view.CameraScreen

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.cameraxapp.ui.components.ManualControls
import com.example.cameraxapp.ui.components.ZoomControl
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

private var imageCapture: ImageCapture? = null
private var videoCapture: VideoCapture<Recorder>? = null
private var recording: Recording? = null
private var cameraExecutor: ExecutorService? = null
private var preview: Preview? = null

private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

// region Compose
@Composable
fun CameraScreen(navController: NavController) {
    val cameraSelection by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    val exposureTime = remember { mutableLongStateOf(10_000_000L) }
    val isoValue = remember { mutableIntStateOf(1000) }

    cameraExecutor = Executors.newSingleThreadExecutor()
    return Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(20.dp)) {
                CameraPreviewViewfinder(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .border(
                            width = 2.dp, color = Color.White, shape = RoundedCornerShape(16.dp)
                        ),
                    cameraSelection = cameraSelection,
                    exposureTime = exposureTime.longValue,
                    isoValue = isoValue.intValue,
                )
            }
            ManualControls(
                onExposureChange = { exposureTime.longValue = it },
                onISOChange = { isoValue.intValue = it },
                onClickCapture = takePhoto(LocalContext.current) )
        }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreviewViewfinder(
    navController: NavController,
    modifier: Modifier,
    cameraSelection: CameraSelector,
    exposureTime: Long,
    isoValue: Int,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }
    var selectedFilter by remember { mutableStateOf("NONE") }

    Column {
        AndroidView(factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }, modifier = modifier.clip(RoundedCornerShape(16.dp)), update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({

                val cameraProvider = cameraProviderFuture.get()

                imageCapture = ImageCapture.Builder().build()

                val recorder = Recorder.Builder().setQualitySelector(
                        QualitySelector.from(
                            Quality.HIGHEST,
                            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                        )
                    ).build()
                videoCapture = VideoCapture.withOutput(recorder)

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

                imageAnalyzer.setAnalyzer(
                    cameraExecutor!!, ColorFilterProcessor(selectedFilter)
                )

                preview = Preview.Builder().apply {
                    Camera2Interop.Extender(this).apply {
                        setCaptureRequestOption(
                            CaptureRequest.CONTROL_EFFECT_MODE,
                            CameraMetadata.CONTROL_EFFECT_MODE_MONO
                        )
                        setCaptureRequestOption(
                            CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime
                        )
                        setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoValue)
                    }
                }.build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelection,
                    preview,
                    imageAnalyzer,
                    imageCapture,
                ).apply {
                    cameraControl = this.cameraControl
                    cameraInfo = this.cameraInfo
                }
            }, ContextCompat.getMainExecutor(context))
        })
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            FilterSelector(onFilterSelected = { filter ->
//                selectedFilter = filter
//            })
            Icon(
                Icons.Default.Photo,
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .clickable { navController.navigate("gallery") },
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
        cameraControl?.let { control ->
            cameraInfo?.let { info ->
                ZoomControl(cameraControl = control, cameraInfo = info)
            }
        }
    }
}

@ComposePreview
@Composable
fun ManualControlsPreview() {
    ManualControls(onExposureChange = {}, onISOChange = {}, onClickCapture = Unit)
}

private fun takePhoto(context: Context) {
    val imageCapture = imageCapture

    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()

    imageCapture?.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.d("CameraScreenLog", "Capture Exception: $exc")
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("CameraScreenLog", "Output: $output")
                Toast.makeText(context, "Photo taken", Toast.LENGTH_SHORT).show()
            }
        })
}

fun captureVideo(context: Context) {
    val videoCapture = videoCapture ?: return

    if (recording != null) {
        recording?.stop()
        recording = null
        return
    }

    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        }
    }

    val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    recording = videoCapture.output.prepareRecording(context, mediaStoreOutputOptions).apply {
            if (PermissionChecker.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            if (recordEvent is VideoRecordEvent.Start) {

            } else if (recordEvent is VideoRecordEvent.Finalize) {
                if (!recordEvent.hasError()) {
                    val msg = "Video capture succeeded: " + "${recordEvent.outputResults.outputUri}"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    recording?.close()
                    recording = null
                }
            }
        }
}
// endregion