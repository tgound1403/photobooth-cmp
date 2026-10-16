package com.example.cameraxapp.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Camera2OverridesTest {

    private val manual = ManualExposure(exposureNs = 1_000_000L, iso = 400)

    @Test
    fun defaultOverridesProduceNoSettings() {
        assertTrue(Camera2Overrides().toSettings(awbLockAvailable = true).isEmpty())
        assertTrue(Camera2Overrides().toSettings(awbLockAvailable = false).isEmpty())
    }

    @Test
    fun manualExposureSetsThreeKeys() {
        val settings = Camera2Overrides(manual = manual).toSettings(awbLockAvailable = false)
        assertEquals(
            setOf(Camera2Setting.AE_MODE_OFF, Camera2Setting.EXPOSURE_TIME, Camera2Setting.SENSITIVITY),
            settings.keys,
        )
        assertEquals(true, settings[Camera2Setting.AE_MODE_OFF])
        assertEquals(1_000_000L, settings[Camera2Setting.EXPOSURE_TIME])
        assertEquals(400, settings[Camera2Setting.SENSITIVITY])
    }

    @Test
    fun awbLockIsDroppedWhenDeviceDoesNotSupportIt() {
        val settings = Camera2Overrides(awbLocked = true).toSettings(awbLockAvailable = false)
        assertTrue(settings.isEmpty())
    }

    @Test
    fun awbLockIsSetWhenSupported() {
        val settings = Camera2Overrides(awbLocked = true).toSettings(awbLockAvailable = true)
        assertEquals(mapOf(Camera2Setting.AWB_LOCK to true), settings)
    }

    @Test
    fun manualAndAwbLockTogetherGiveFourKeys() {
        val settings =
            Camera2Overrides(awbLocked = true, manual = manual).toSettings(awbLockAvailable = true)
        assertEquals(4, settings.size)
        assertEquals(true, settings[Camera2Setting.AWB_LOCK])
    }
}
