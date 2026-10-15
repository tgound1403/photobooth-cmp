package com.example.cameraxapp.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CaptureRequest
import android.media.ExifInterface
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.cameraxapp.core.extension.getCameraProvider
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.FilterCatalog
import com.example.cameraxapp.core.render.FilterSurfaceProcessor
import com.example.cameraxapp.core.render.GlRenderer
import java.io.File
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Use cases and camera of one bind. */
data class BoundCamera(val preview: Preview, val imageCapture: ImageCapture, val camera: Camera)

/**
 * The only owner of the CameraX session: binds use cases once, applies the live filter, takes
 * photos, and sets manual exposure. Callers never bind on their own.
 *
 * Filter: a single [FilterSurfaceProcessor] is attached as a CameraEffect targeting PREVIEW and
 * IMAGE_CAPTURE. [setFilter] swaps the edit for the next frames without rebinding. If the device
 * refuses the capture effect, the service binds with a PREVIEW-only effect and [takePhoto] applies
 * the edit to the saved JPEG with [GlRenderer]. Each capture logs which path was used.
 */
class CameraService(private val catalog: FilterCatalog) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val processor = FilterSurfaceProcessor()
    private var renderer: GlRenderer? = null

    @Volatile
    private var requested = EditParams()

    @Volatile
    private var edit = EditParams()

    @Volatile
    private var lut: Bitmap? = null

    @Volatile
    private var captureUsesEffect = false

    private var imageCapture: ImageCapture? = null

    /**
     * Binds preview and image capture to [lifecycleOwner] with the camera facing [lensFacing].
     * Must run on the main thread. Replaces any previous binding.
     */
    suspend fun bind(context: Context, lifecycleOwner: LifecycleOwner, lensFacing: Int): BoundCamera {
        val app = context.applicationContext
        val provider = app.getCameraProvider()
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val executor = ContextCompat.getMainExecutor(app)

        provider.unbindAll()
        return try {
            val bound = bindUseCases(
                app, provider, lifecycleOwner, selector, executor,
                CameraEffect.PREVIEW or CameraEffect.IMAGE_CAPTURE,
            )
            captureUsesEffect = true
            Log.i(TAG, "Filter effect bound to preview and image capture")
            bound
        } catch (e: Exception) {
            Log.w(TAG, "Filter effect on image capture rejected, using preview-only effect: ${e.message}", e)
            provider.unbindAll()
            captureUsesEffect = false
            bindUseCases(app, provider, lifecycleOwner, selector, executor, CameraEffect.PREVIEW)
        }
    }

    private fun bindUseCases(
        app: Context,
        provider: androidx.camera.lifecycle.ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        executor: Executor,
        effectTargets: Int,
    ): BoundCamera {
        val preview = createPreview()
        val capture = createImageCapture()
        val effect = object : CameraEffect(
            effectTargets,
            executor,
            processor,
            { error -> Log.e(TAG, "Camera effect error", error) },
        ) {}
        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(capture)
            .addEffect(effect)
            .build()
        val camera = provider.bindToLifecycle(lifecycleOwner, selector, group)
        imageCapture = capture
        return BoundCamera(preview, capture, camera)
    }

    /**
     * Sets the edit used for following frames and captures. Does not rebind the camera. The LUT is
     * decoded off the main thread. The latest call always wins.
     */
    fun setFilter(params: EditParams) {
        requested = params
        scope.launch(Dispatchers.IO) {
            val p = requested.clamped()
            val bitmap = catalog.loadLut(p.filterId)
            edit = p
            lut = bitmap
            processor.setEdit(p, bitmap)
        }
    }

    /** Manual exposure (ns) and ISO through Camera2 request options. Applied live, no rebind. */
    fun setManualExposure(camera: Camera, exposureNs: Long, iso: Int) {
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
            .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
            .build()
        Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
    }

    /**
     * Takes a photo into [file]. [onSaved] runs on the main thread once the file holds the final,
     * filtered JPEG (EXIF orientation kept). [onFailure] runs on failure.
     */
    fun takePhoto(
        file: File,
        executor: Executor,
        onSaved: (File) -> Unit,
        onFailure: (ImageCaptureException) -> Unit,
    ) {
        val capture = imageCapture
        if (capture == null) {
            onFailure(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Camera is not ready", null))
            return
        }
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    if (captureUsesEffect) {
                        Log.d(TAG, "Capture path: GPU effect (already filtered)")
                        onSaved(file)
                        return
                    }
                    Log.d(TAG, "Capture path: fallback (raw capture, then GlRenderer)")
                    scope.launch {
                        try {
                            applyEditToFile(file)
                            withContext(Dispatchers.Main) { onSaved(file) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Fallback filter failed", e)
                            withContext(Dispatchers.Main) {
                                onFailure(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Filter failed", e))
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onFailure(exception)
                }
            },
        )
    }

    /** Re-renders the JPEG at [file] with the current edit, keeping its EXIF orientation. */
    private suspend fun applyEditToFile(file: File) {
        val params = edit
        if (params.isIdentity) return
        val source = BitmapFactory.decodeFile(file.absolutePath)
            ?: error("Cannot decode captured photo ${file.name}")
        val orientation = ExifInterface(file.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val gl = renderer ?: GlRenderer().also { renderer = it }
        val rendered = gl.render(source, params, lut, maxSize = MAX_FALLBACK_SIZE)
        source.recycle()
        file.outputStream().use { rendered.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        rendered.recycle()

        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
    }

    /** Stops the filter pipeline and frees GL resources. Call when the owner is destroyed. */
    fun shutdown() {
        scope.cancel()
        processor.release()
        renderer?.close()
        renderer = null
    }

    private fun createPreview(): Preview {
        return Preview.Builder()
            .setResolutionSelector(defaultResolutionSelector())
            .build()
    }

    private fun createImageCapture(): ImageCapture {
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(defaultResolutionSelector())
            .build()
    }

    private fun defaultResolutionSelector(): ResolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_NONE)
            )
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()

    private companion object {
        const val TAG = "CameraService"
        const val JPEG_QUALITY = 95
        const val MAX_FALLBACK_SIZE = 4096
    }
}
