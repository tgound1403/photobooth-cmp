package com.example.photogether.core.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class HorizontalDialMathTest {
    @Test fun zeroDragKeepsIndex() = assertEquals(5, detentForDrag(5, 0f, 20f, 10))

    @Test fun dragRightIncreases() = assertEquals(7, detentForDrag(5, 40f, 20f, 10))

    @Test fun dragLeftDecreases() = assertEquals(3, detentForDrag(5, -40f, 20f, 10))

    @Test fun clampsAtBothEnds() {
        assertEquals(9, detentForDrag(8, 500f, 20f, 10))
        assertEquals(0, detentForDrag(1, -500f, 20f, 10))
    }

    @Test fun roundsAtHalfDetent() {
        assertEquals(5, detentForDrag(5, 9f, 20f, 10))
        assertEquals(6, detentForDrag(5, 11f, 20f, 10))
        assertEquals(4, detentForDrag(5, -11f, 20f, 10))
    }

    @Test fun singleDetentStaysAtZero() {
        assertEquals(0, detentForDrag(0, 300f, 20f, 1))
        assertEquals(0, detentForDrag(0, -300f, 20f, 1))
    }
}
