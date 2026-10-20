package com.example.photogether.core.moments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShotSettingsTest {

    @Test
    fun zero_ev_is_zero_over_hundred() {
        assertEquals("0/100", evAsRational(0f))
    }

    @Test
    fun positive_ev_keeps_its_sign_and_two_decimals() {
        assertEquals("30/100", evAsRational(0.3f))
        assertEquals("67/100", evAsRational(0.67f))
    }

    @Test
    fun negative_ev_keeps_its_sign() {
        assertEquals("-150/100", evAsRational(-1.5f))
    }

    @Test
    fun user_comment_round_trips_kelvin_and_strength() {
        val text = encodeUserComment(5500, 72)
        assertEquals("kelvin=5500;intensity=72", text)
        assertEquals(UserCommentValues(5500, 72), decodeUserComment(text))
    }

    @Test
    fun user_comment_leaves_out_missing_fields() {
        assertEquals("intensity=40", encodeUserComment(null, 40))
        assertEquals(UserCommentValues(null, 40), decodeUserComment("intensity=40"))
        assertEquals("kelvin=3200", encodeUserComment(3200, null))
        assertNull(encodeUserComment(null, null))
    }

    @Test
    fun legacy_user_comment_of_plain_digits_is_kelvin() {
        assertEquals(UserCommentValues(5500, null), decodeUserComment("5500"))
    }

    @Test
    fun blank_or_unknown_user_comment_gives_nulls() {
        assertEquals(UserCommentValues(null, null), decodeUserComment(null))
        assertEquals(UserCommentValues(null, null), decodeUserComment("  "))
        assertEquals(UserCommentValues(null, null), decodeUserComment("hello"))
    }

    @Test
    fun strength_is_clamped_to_a_percent() {
        assertEquals(UserCommentValues(null, 100), decodeUserComment("intensity=250"))
    }

    @Test
    fun user_comment_round_trips_tint_with_the_other_values() {
        val text = encodeUserComment(kelvin = 3200, intensityPercent = 72, tintPercent = -40)
        assertEquals("kelvin=3200;intensity=72;tint=-40", text)
        val decoded = decodeUserComment(text)
        assertEquals(-40, decoded.tintPercent)
        assertEquals(3200, decoded.kelvin)
        assertEquals(72, decoded.intensityPercent)
    }

    @Test
    fun tint_alone_is_written_and_clamped_to_a_hundred_percent() {
        assertEquals("tint=25", encodeUserComment(kelvin = null, intensityPercent = null, tintPercent = 25))
        assertEquals(100, decodeUserComment("tint=250").tintPercent)
        assertEquals(-100, decodeUserComment("tint=-250").tintPercent)
    }

    @Test
    fun user_comment_without_tint_reads_tint_as_null() {
        assertNull(decodeUserComment("kelvin=5500;intensity=50").tintPercent)
    }
}
