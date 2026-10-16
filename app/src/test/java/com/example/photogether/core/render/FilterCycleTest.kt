package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterCycleTest {

    private val ids = listOf("original", "warm", "cool", "mono")

    @Test
    fun forwardVisitsEveryFilterInOrderAndWraps() {
        val visited = mutableListOf("original")
        repeat(ids.size) {
            visited += FilterCycle.next(ids, visited.last(), 1)
        }
        assertEquals(listOf("original", "warm", "cool", "mono", "original"), visited)
    }

    @Test
    fun backwardWrapsFromFirstToLast() {
        assertEquals("mono", FilterCycle.next(ids, "original", -1))
        assertEquals("cool", FilterCycle.next(ids, "mono", -1))
    }

    @Test
    fun largeDeltaWrapsModulo() {
        assertEquals("cool", FilterCycle.next(ids, "original", 6))
        assertEquals("warm", FilterCycle.next(ids, "original", -7))
    }

    @Test
    fun zeroDeltaKeepsCurrent() {
        assertEquals("cool", FilterCycle.next(ids, "cool", 0))
    }

    @Test
    fun unknownCurrentStartsFromAnEnd() {
        assertEquals("original", FilterCycle.next(ids, "missing", 1))
        assertEquals("mono", FilterCycle.next(ids, "missing", -1))
    }

    @Test
    fun emptyListReturnsCurrent() {
        assertEquals("original", FilterCycle.next(emptyList(), "original", 1))
    }
}
