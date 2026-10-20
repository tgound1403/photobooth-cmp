package com.example.photogether.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualExposureStepsTest {

    @Test
    fun isoStepsStayInsideTheSensorRange() {
        val steps = ManualExposureSteps.isoSteps(100..3200)
        assertEquals(100, steps.first())
        assertEquals(3200, steps.last())
        assertTrue(steps.all { it in 100..3200 })
    }

    @Test
    fun isoStepsAreAscending() {
        val steps = ManualExposureSteps.isoSteps(50..12800)
        assertEquals(steps.sorted(), steps)
    }

    @Test
    fun isoStepsFallBackToTheLowerEndWhenNoStopFits() {
        assertEquals(listOf(101), ManualExposureSteps.isoSteps(101..102))
    }

    @Test
    fun shutterStepsAreFastestFirstAndInsideTheRange() {
        val range = 125_000L..1_000_000_000L
        val steps = ManualExposureSteps.shutterSteps(range)
        assertEquals(steps.sorted(), steps)
        assertTrue(steps.all { it in range })
        assertEquals("1/8000", ManualExposureSteps.shutterLabel(steps.first()))
        assertEquals("1\"", ManualExposureSteps.shutterLabel(steps.last()))
    }

    @Test
    fun shutterStepsAreCutByTheSensorMaximum() {
        val steps = ManualExposureSteps.shutterSteps(100_000L..16_666_667L)
        assertEquals("1/60", ManualExposureSteps.shutterLabel(steps.last()))
    }

    @Test
    fun shutterLabels() {
        assertEquals("1/250", ManualExposureSteps.shutterLabel(4_000_000L))
        assertEquals("1/3", ManualExposureSteps.shutterLabel(333_333_333L))
        assertEquals("0.5\"", ManualExposureSteps.shutterLabel(500_000_000L))
        assertEquals("1\"", ManualExposureSteps.shutterLabel(1_000_000_000L))
    }

    @Test
    fun nearestIndexPicksTheClosestValue() {
        val steps = ManualExposureSteps.isoSteps(100..1600)
        assertEquals(steps.indexOf(400), ManualExposureSteps.nearestIndex(steps, 420))
        assertEquals(0, ManualExposureSteps.nearestIndex(steps, 1))
        assertEquals(steps.lastIndex, ManualExposureSteps.nearestIndex(steps, 99999))
    }
}
