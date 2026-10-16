package com.example.cameraxapp.core.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTargetTest {

    private val send = "android.intent.action.SEND"

    @Test
    fun packageNamesMatchTheBrief() {
        assertEquals("com.zhiliaoapp.musically", ShareTarget.TIKTOK.packageName)
        assertEquals("com.instagram.android", ShareTarget.INSTAGRAM.packageName)
        assertEquals("com.instagram.android", ShareTarget.IG_STORY.packageName)
        assertEquals("com.facebook.katana", ShareTarget.FACEBOOK.packageName)
        assertEquals("com.facebook.orca", ShareTarget.MESSENGER.packageName)
        assertNull(ShareTarget.OTHER.packageName)
    }

    @Test
    fun igStoryUsesStoryActionAndOthersUseSend() {
        assertEquals("com.instagram.share.ADD_TO_STORY", ShareTarget.IG_STORY.action)
        assertEquals(send, ShareTarget.TIKTOK.action)
        assertEquals(send, ShareTarget.INSTAGRAM.action)
        assertEquals(send, ShareTarget.FACEBOOK.action)
        assertEquals(send, ShareTarget.MESSENGER.action)
        assertEquals(send, ShareTarget.OTHER.action)
    }

    @Test
    fun labelsAreShownAsSpecified() {
        assertEquals("TikTok", ShareTarget.TIKTOK.label)
        assertEquals("Instagram", ShareTarget.INSTAGRAM.label)
        assertEquals("IG Story", ShareTarget.IG_STORY.label)
        assertEquals("Facebook", ShareTarget.FACEBOOK.label)
        assertEquals("Messenger", ShareTarget.MESSENGER.label)
        assertEquals("Khác", ShareTarget.OTHER.label)
    }

    @Test
    fun otherAlwaysUsesChooser() {
        assertTrue(usesChooser(ShareTarget.OTHER, emptySet()))
        assertTrue(usesChooser(ShareTarget.OTHER, setOf("com.facebook.orca")))
    }

    @Test
    fun missingPackageUsesChooser() {
        assertTrue(usesChooser(ShareTarget.MESSENGER, emptySet()))
        assertTrue(usesChooser(ShareTarget.TIKTOK, setOf("com.facebook.orca")))
    }

    @Test
    fun installedPackageSkipsChooser() {
        assertFalse(usesChooser(ShareTarget.MESSENGER, setOf("com.facebook.orca")))
        assertFalse(usesChooser(ShareTarget.TIKTOK, setOf("com.zhiliaoapp.musically")))
    }

    @Test
    fun igStoryFollowsInstagramInstallState() {
        assertTrue(usesChooser(ShareTarget.IG_STORY, emptySet()))
        assertFalse(usesChooser(ShareTarget.IG_STORY, setOf("com.instagram.android")))
    }

    @Test
    fun packagesAreUniqueExceptInstagramPair() {
        val byPackage =
            ShareTarget.entries.filter { it.packageName != null }.groupBy { it.packageName }

        for ((pkg, targets) in byPackage) {
            if (pkg == "com.instagram.android") {
                assertEquals(setOf(ShareTarget.INSTAGRAM, ShareTarget.IG_STORY), targets.toSet())
            } else {
                assertEquals("package $pkg should appear once", 1, targets.size)
            }
        }
    }
}
