package com.example.photogether.ui.onboarding

import com.example.photogether.ui.onboarding.OnboardingStep.INTRO
import com.example.photogether.ui.onboarding.OnboardingStep.LANGUAGE
import com.example.photogether.ui.onboarding.OnboardingStep.PERMISSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingFlowTest {
    @Test fun firstRunWalksLanguageIntroPermission() {
        assertEquals(LANGUAGE, OnboardingFlow.start(replay = false))
        assertEquals(INTRO, OnboardingFlow.next(LANGUAGE, replay = false))
        assertEquals(PERMISSION, OnboardingFlow.next(INTRO, replay = false))
        assertNull(OnboardingFlow.next(PERMISSION, replay = false))
    }

    @Test fun replayOnlyShowsIntro() {
        assertEquals(INTRO, OnboardingFlow.start(replay = true))
        assertNull(OnboardingFlow.next(INTRO, replay = true))
    }

    @Test fun skipGoesToPermissionOrEndsReplay() {
        assertEquals(PERMISSION, OnboardingFlow.skip(replay = false))
        assertNull(OnboardingFlow.skip(replay = true))
    }

    @Test fun nextPageStopsAtLastPage() {
        assertEquals(1, OnboardingFlow.nextPage(0))
        assertEquals(INTRO_PAGE_COUNT - 1, OnboardingFlow.nextPage(INTRO_PAGE_COUNT - 2))
        assertNull(OnboardingFlow.nextPage(INTRO_PAGE_COUNT - 1))
    }
}
