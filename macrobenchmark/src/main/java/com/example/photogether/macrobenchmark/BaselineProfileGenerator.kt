package com.example.photogether.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Generates the baseline profile covering startup and the main scrolling screens. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = StartupBenchmark.TARGET_PACKAGE,
        includeInStartupProfile = true,
    ) {
        grantAppPermissions()
        pressHome()
        launchMainActivity()
        device.findObject(By.desc(ScrollBenchmark.MOMENTS_DESC))?.click()
        device.wait(Until.hasObject(By.scrollable(true)), ScrollBenchmark.TIMEOUT_MS)
        device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        device.waitForIdle()
    }
}
