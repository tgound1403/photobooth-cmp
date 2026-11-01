package com.example.photogether.core.tutorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TutorialStepTest {

    @Test
    fun `tour walks camera, gallery, then calendar and ends`() {
        val order = generateSequence(TutorialStep.entries.first()) { it.next }.toList()
        assertEquals(
            listOf(
                TutorialStep.HOME_CAMERA,
                TutorialStep.CAPTURE,
                TutorialStep.FIRST_SHOT,
                TutorialStep.LOOK,
                TutorialStep.ADJUST,
                TutorialStep.OPEN_GALLERY,
                TutorialStep.SHOOT_MORE,
                TutorialStep.OPEN_CALENDAR_DAY,
                TutorialStep.FINISH,
            ),
            order,
        )
        assertNull(TutorialStep.FINISH.next)
    }

    @Test
    fun `stored name round-trips and unknown names give no step`() {
        TutorialStep.entries.forEach { assertEquals(it, TutorialStep.fromName(it.name)) }
        assertNull(TutorialStep.fromName(null))
        assertNull(TutorialStep.fromName("REMOVED_STEP"))
    }

    @Test
    fun `only the reward and finish cards have no control to point at`() {
        assertEquals(
            listOf(TutorialStep.FIRST_SHOT, TutorialStep.FINISH),
            TutorialStep.entries.filter { it.target == null },
        )
    }

    @Test
    fun `passive steps are the reward, the tip and the finish`() {
        assertEquals(
            listOf(TutorialStep.FIRST_SHOT, TutorialStep.ADJUST, TutorialStep.FINISH),
            TutorialStep.entries.filter { it.isPassive },
        )
        assertEquals(listOf(TutorialStep.ADJUST), TutorialStep.entries.filter { it.isTip })
    }
}
