package com.example.photogether.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing for the screens that will get the most new animation:
 * the 3D camera picker (spin) and the moments grid (scroll).
 * Selectors are best-effort on the Home screen; adjust if the UI labels change.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun momentsScroll() = benchmarkRule.measureRepeated(
        packageName = StartupBenchmark.TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            grantAppPermissions()
            pressHome()
            launchMainActivity()
        },
    ) {
        val folder = device.wait(Until.findObject(By.desc(MOMENTS_DESC)), TIMEOUT_MS)
        requireNotNull(folder) { "Moments folder not found on Home" }
        folder.click()
        requireNotNull(device.wait(Until.findObject(By.scrollable(true)), TIMEOUT_MS)) {
            "Moments grid not found after tapping the folder"
        }
        // The grid recomposes while thumbnails load, so re-find it before each gesture.
        device.findObject(By.scrollable(true)).fling(Direction.DOWN)
        device.waitForIdle()
        device.findObject(By.scrollable(true)).fling(Direction.UP)
        device.waitForIdle()
        // A warm restart resumes on this screen, so return to Home for the next iteration.
        device.pressBack()
        device.wait(Until.hasObject(By.desc(MOMENTS_DESC)), TIMEOUT_MS)
    }

    @Test
    fun cameraPickerSpin() = benchmarkRule.measureRepeated(
        packageName = StartupBenchmark.TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            grantAppPermissions()
            pressHome()
            launchMainActivity()
        },
    ) {
        device.findObject(By.desc(CAMERA_BUTTON_DESC))?.click()
        device.wait(Until.hasObject(By.descStartsWith(PICKER_ENTRY_PREFIX)), TIMEOUT_MS)
        device.findObject(By.descStartsWith(PICKER_ENTRY_PREFIX))?.click()
        device.wait(Until.hasObject(By.clazz("android.view.SurfaceView")), TIMEOUT_MS)
        device.findObject(By.clazz("android.view.SurfaceView"))?.swipe(
            Direction.LEFT,
            0.8f,
        )
        device.waitForIdle()
    }

    companion object {
        const val MOMENTS_DESC = "Moments"
        const val CAMERA_BUTTON_DESC = "Máy ảnh"
        const val PICKER_ENTRY_PREFIX = "Chọn máy"
        const val TIMEOUT_MS = 5_000L
    }
}
