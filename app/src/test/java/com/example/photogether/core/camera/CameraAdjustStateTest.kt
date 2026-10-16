package com.example.photogether.core.camera

import com.example.photogether.core.render.EditParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraAdjustStateTest {

    @Test
    fun withCameraAdjustInHardwareModeZeroesSoftwareEv() {
        val state = CameraAdjustState(ev = 0.7f, evMode = EvMode.HARDWARE)
        val params = EditParams().withCameraAdjust(state)
        assertEquals(0f, params.softwareEv, 0f)
    }

    @Test
    fun withCameraAdjustInSoftwareModeCopiesEv() {
        val state = CameraAdjustState(ev = 0.7f, evMode = EvMode.SOFTWARE)
        val params = EditParams().withCameraAdjust(state)
        assertEquals(0.7f, params.softwareEv, 1e-5f)
    }

    @Test
    fun withCameraAdjustCopiesKelvin() {
        val params = EditParams().withCameraAdjust(CameraAdjustState().withKelvin(3200))
        assertEquals(3200, params.kelvin)
        assertEquals(null, EditParams().withCameraAdjust(CameraAdjustState()).kelvin)
    }

    @Test
    fun withEvSnapsToStep() {
        val state = CameraAdjustState()
        assertEquals(1f / 3f, state.withEv(0.4f).ev, 1e-4f)
        assertEquals(1f, state.withEv(0.9f).ev, 1e-4f)
    }

    @Test
    fun withEvClampsToRange() {
        assertEquals(2f, CameraAdjustState().withEv(10f).ev, 1e-4f)
        assertEquals(-2f, CameraAdjustState().withEv(-10f).ev, 1e-4f)
    }

    @Test
    fun withCapabilitiesWithoutHardwareEvFallsBackToSoftware() {
        val hardwareRange = EvRange(minIndex = -3, maxIndex = 3, step = 1f / 6f)
        val state =
            CameraAdjustState(evMode = EvMode.HARDWARE, evRange = hardwareRange)
                .withCapabilities(CameraCapabilities(evRange = null, awbLockAvailable = true))
        assertEquals(EvMode.SOFTWARE, state.evMode)
        assertEquals(EvRange.SOFTWARE, state.evRange)
        assertTrue(state.awbLockAvailable)
    }

    @Test
    fun withCapabilitiesWithHardwareEvUsesDeviceRange() {
        val hardwareRange = EvRange(minIndex = -3, maxIndex = 3, step = 1f / 6f)
        val state =
            CameraAdjustState().withCapabilities(
                CameraCapabilities(evRange = hardwareRange, awbLockAvailable = false),
            )
        assertEquals(EvMode.HARDWARE, state.evMode)
        assertEquals(hardwareRange, state.evRange)
        assertFalse(state.awbLockAvailable)
    }

    @Test
    fun withCapabilitiesResnapsCurrentEv() {
        val coarse = EvRange(minIndex = -2, maxIndex = 2, step = 0.5f)
        val state =
            CameraAdjustState(ev = 0.3333f)
                .withCapabilities(CameraCapabilities(evRange = coarse, awbLockAvailable = false))
        assertEquals(0.5f, state.ev, 1e-5f)
    }

    @Test
    fun overridesLockAwbOnlyWhenKelvinSetAndSupported() {
        val auto = CameraAdjustState(awbLockAvailable = true)
        assertFalse(auto.overrides(manual = null).awbLocked)

        val manualNoSupport = CameraAdjustState(kelvin = 3200, awbLockAvailable = false)
        assertFalse(manualNoSupport.overrides(manual = null).awbLocked)

        val manualSupported = CameraAdjustState(kelvin = 3200, awbLockAvailable = true)
        assertTrue(manualSupported.overrides(manual = null).awbLocked)
    }

    @Test
    fun overridesPassesManualExposureThrough() {
        val manual = ManualExposure(exposureNs = 2_000_000L, iso = 200)
        assertEquals(manual, CameraAdjustState().overrides(manual).manual)
    }

    @Test
    fun withKelvinClampsToEditRange() {
        assertEquals(10_000, CameraAdjustState().withKelvin(99_999).kelvin)
        assertEquals(2_000, CameraAdjustState().withKelvin(100).kelvin)
        assertEquals(null, CameraAdjustState(kelvin = 3200).withKelvin(null).kelvin)
    }
}
