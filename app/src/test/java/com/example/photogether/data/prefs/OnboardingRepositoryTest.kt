package com.example.photogether.data.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRepositoryTest {
    @Test fun notCompletedWithoutStoredVersion() {
        assertFalse(OnboardingRepository.isCompleted(null))
        assertFalse(OnboardingRepository.isCompleted(0))
    }

    @Test fun completedAtOrAboveCurrentVersion() {
        assertTrue(OnboardingRepository.isCompleted(OnboardingRepository.CURRENT_VERSION))
        assertTrue(OnboardingRepository.isCompleted(OnboardingRepository.CURRENT_VERSION + 1))
    }
}
