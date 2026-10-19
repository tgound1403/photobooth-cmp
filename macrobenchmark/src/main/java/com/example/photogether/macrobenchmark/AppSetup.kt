package com.example.photogether.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/**
 * The app asks for camera, microphone and media access on first launch. A fresh install
 * shows those dialogs over the activity, so grant them up front.
 */
fun MacrobenchmarkScope.grantAppPermissions() {
    listOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        // READ_MEDIA_IMAGES exists from Android 13; older devices (e.g. Pixel 3 on 12) use READ_EXTERNAL_STORAGE.
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_EXTERNAL_STORAGE",
    ).forEach { permission ->
        device.executeShellCommand("pm grant ${StartupBenchmark.TARGET_PACKAGE} $permission")
    }
}

/**
 * Launches MainActivity with `am start -W` and waits for its window to appear.
 * Used instead of startActivityAndWait(): on the emulator that helper throws
 * "Unable to confirm activity launch completion" even though the activity is resumed
 * and drawing frames. Root cause not yet confirmed.
 */
fun MacrobenchmarkScope.launchMainActivity() {
    device.executeShellCommand(
        "am start -W -n ${StartupBenchmark.TARGET_PACKAGE}/$MAIN_ACTIVITY",
    )
    device.wait(Until.hasObject(By.pkg(StartupBenchmark.TARGET_PACKAGE).depth(0)), TIMEOUT_MS)
    device.waitForIdle()
}

private const val MAIN_ACTIVITY = "com.example.photogether.MainActivity"
private const val TIMEOUT_MS = 10_000L
