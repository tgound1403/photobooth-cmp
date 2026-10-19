package com.example.photogether.core.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test fun appChoiceWinsOverSystem() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve("en", "vi"))
    }

    @Test fun fallsBackToSystemThenEnglish() {
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.resolve(null, "vi"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(null, "fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(null, null))
    }

    @Test fun fromTagIsCaseInsensitiveAndRejectsUnknown() {
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.fromTag("VI"))
        assertNull(AppLanguage.fromTag("de"))
    }
}
