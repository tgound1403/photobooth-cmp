package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditParamsTest {

    @Test
    fun jsonRoundTripEqualsOriginal() {
        val original =
            EditParams(
                filterId = "golden_hour",
                filterIntensity = 0.6f,
                exposure = 0.25f,
                contrast = -0.3f,
                saturation = 0.1f,
                temperature = 0.4f,
                tint = -0.2f,
                highlight = 0.15f,
                shadow = -0.5f,
                fade = 0.2f,
                sharpness = 0.7f,
                grain = 0.3f,
                vignette = 0.1f,
            )
        assertEquals(original, EditParams.fromJson(original.toJson()))
    }

    @Test
    fun emptyJsonGivesDefaults() {
        assertEquals(EditParams(), EditParams.fromJson("{}"))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val json = """{"filterId":"kodak","someFutureField":42,"nested":{"a":1}}"""
        val params = EditParams.fromJson(json)
        assertEquals("kodak", params.filterId)
        assertEquals(EditParams(filterId = "kodak"), params)
    }

    @Test
    fun clampedClampsOutOfRangeValues() {
        val wild =
            EditParams(
                filterIntensity = 3f,
                exposure = -5f,
                contrast = 2f,
                saturation = -2f,
                temperature = 9f,
                tint = -9f,
                highlight = 1.5f,
                shadow = -1.5f,
                fade = 4f,
                sharpness = -1f,
                grain = 7f,
                vignette = -0.5f,
            )
        val clamped = wild.clamped()
        assertEquals(1f, clamped.filterIntensity, 0f)
        assertEquals(-1f, clamped.exposure, 0f)
        assertEquals(1f, clamped.contrast, 0f)
        assertEquals(-1f, clamped.saturation, 0f)
        assertEquals(1f, clamped.temperature, 0f)
        assertEquals(-1f, clamped.tint, 0f)
        assertEquals(1f, clamped.highlight, 0f)
        assertEquals(-1f, clamped.shadow, 0f)
        assertEquals(1f, clamped.fade, 0f)
        assertEquals(0f, clamped.sharpness, 0f)
        assertEquals(1f, clamped.grain, 0f)
        assertEquals(0f, clamped.vignette, 0f)
        assertEquals("original", clamped.filterId)
    }

    @Test
    fun clampedKeepsInRangeValues() {
        val params = EditParams(filterId = "nikon", filterIntensity = 0.5f, exposure = -0.5f)
        assertEquals(params, params.clamped())
    }

    @Test
    fun isIdentityTrueForDefaults() {
        assertTrue(EditParams().isIdentity)
    }

    @Test
    fun isIdentityTrueForZeroIntensityWithFilter() {
        assertTrue(EditParams(filterId = "kodak", filterIntensity = 0f).isIdentity)
    }

    @Test
    fun isIdentityFalseWhenFilterApplied() {
        assertFalse(EditParams(filterId = "kodak").isIdentity)
    }

    @Test
    fun isIdentityFalseAfterChangingAnyAdjustment() {
        val changed =
            listOf(
                EditParams(exposure = 0.1f),
                EditParams(contrast = 0.1f),
                EditParams(saturation = -0.1f),
                EditParams(temperature = 0.1f),
                EditParams(tint = 0.1f),
                EditParams(highlight = 0.1f),
                EditParams(shadow = 0.1f),
                EditParams(fade = 0.1f),
                EditParams(sharpness = 0.1f),
                EditParams(grain = 0.1f),
                EditParams(vignette = 0.1f),
            )
        changed.forEach { assertFalse(it.toString(), it.isIdentity) }
    }

    @Test
    fun legacyJsonWithoutKelvinDecodesToAuto() {
        val params = EditParams.fromJson("""{"filterId":"kodak","temperature":0.4}""")
        assertEquals(null, params.kelvin)
        assertEquals(0f, params.softwareEv, 0f)
    }

    @Test
    fun toJsonOmitsKelvinAndSoftwareEvWhenUnset() {
        val json = EditParams().toJson()
        assertFalse(json, json.contains("kelvin"))
        assertFalse(json, json.contains("softwareEv"))
    }

    @Test
    fun kelvinRoundTrip() {
        val original = EditParams(kelvin = 3200)
        val decoded = EditParams.fromJson(original.toJson())
        assertEquals(3200, decoded.kelvin)
        assertEquals(original, decoded)
    }

    @Test
    fun isIdentityFalseWhenKelvinSet() {
        assertFalse(EditParams(kelvin = 3200).isIdentity)
    }

    @Test
    fun isIdentityFalseWhenSoftwareEvSet() {
        assertFalse(EditParams(softwareEv = 0.3f).isIdentity)
    }

    @Test
    fun clampedClampsKelvinAndSoftwareEv() {
        assertEquals(10000, EditParams(kelvin = 99999).clamped().kelvin)
        assertEquals(2000, EditParams(kelvin = 100).clamped().kelvin)
        assertEquals(null, EditParams(kelvin = null).clamped().kelvin)
        assertEquals(2f, EditParams(softwareEv = 5f).clamped().softwareEv, 0f)
        assertEquals(-2f, EditParams(softwareEv = -5f).clamped().softwareEv, 0f)
    }
}
