package com.example.photogether.core.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class RotaryMathTest {

    @Test
    fun angleIsZeroToTheRightAndGrowsClockwise() {
        assertEquals(0f, angleDegrees(10f, 0f, 0f, 0f), 0.01f)
        assertEquals(90f, angleDegrees(0f, 10f, 0f, 0f), 0.01f)
        assertEquals(-90f, angleDegrees(0f, -10f, 0f, 0f), 0.01f)
    }

    @Test
    fun shortestTurnCrossesTheSeamAsASmallStep() {
        assertEquals(20f, shortestTurn(170f, -170f), 0.01f)
        assertEquals(-20f, shortestTurn(-170f, 170f), 0.01f)
        assertEquals(15f, shortestTurn(10f, 25f), 0.01f)
    }

    @Test
    fun shortestTurnStaysInHalfOpenRange() {
        assertEquals(180f, shortestTurn(0f, 180f), 0.01f)
        assertEquals(180f, shortestTurn(0f, -180f), 0.01f)
    }

    @Test
    fun detentIndexRoundsToNearest() {
        assertEquals(0, detentIndexFor(4f, 10f, 5))
        assertEquals(1, detentIndexFor(6f, 10f, 5))
        assertEquals(3, detentIndexFor(29f, 10f, 5))
    }

    @Test
    fun detentIndexClampsToTheEnds() {
        assertEquals(0, detentIndexFor(-50f, 10f, 5))
        assertEquals(4, detentIndexFor(500f, 10f, 5))
        assertEquals(0, detentIndexFor(30f, 10f, 1))
    }

    @Test
    fun detentAngleMatchesIndex() {
        assertEquals(30f, detentAngle(3, 10f), 0f)
        assertEquals(3, detentIndexFor(detentAngle(3, 10f), 10f, 5))
    }
}
