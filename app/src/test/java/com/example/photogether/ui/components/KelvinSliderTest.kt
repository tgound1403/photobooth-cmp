package com.example.photogether.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class KelvinSliderTest {

    @Test
    fun labelShowsAutoForNull() {
        assertEquals("Auto", kelvinLabel(null))
    }

    @Test
    fun labelShowsKelvinSuffix() {
        assertEquals("5500K", kelvinLabel(5500))
        assertEquals("2000K", kelvinLabel(2000))
    }
}
