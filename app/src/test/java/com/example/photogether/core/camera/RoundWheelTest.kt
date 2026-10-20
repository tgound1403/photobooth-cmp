package com.example.photogether.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundWheelTest {
    @Test
    fun defaultsToEvForMissingOrUnknownKeys() {
        assertEquals(RoundWheel.EV, RoundWheel.fromKey(null))
        assertEquals(RoundWheel.EV, RoundWheel.fromKey("nonsense"))
    }

    @Test
    fun keysRoundTrip() {
        RoundWheel.entries.forEach { assertEquals(it, RoundWheel.fromKey(it.key)) }
    }

    @Test
    fun theOtherTwoControlsBecomeDialsInShutterIsoEvOrder() {
        assertEquals(listOf(RoundWheel.TV, RoundWheel.ISO), RoundWheel.EV.dials())
        assertEquals(listOf(RoundWheel.TV, RoundWheel.EV), RoundWheel.ISO.dials())
        assertEquals(listOf(RoundWheel.ISO, RoundWheel.EV), RoundWheel.TV.dials())
    }

    @Test
    fun theWheelIsNeverAlsoADial() {
        RoundWheel.entries.forEach { assertTrue(it !in it.dials()) }
    }
}
