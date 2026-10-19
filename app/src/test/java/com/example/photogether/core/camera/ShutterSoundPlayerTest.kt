package com.example.photogether.core.camera

import android.media.AudioManager
import com.example.photogether.core.render.CameraProfiles
import com.example.photogether.core.render.ShutterSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShutterSoundPlayerTest {
    @Test fun normalRingerPlaysWhenEnabled() {
        assertTrue(shouldPlay(enabled = true, ringerMode = AudioManager.RINGER_MODE_NORMAL))
    }

    @Test fun silentAndVibrateStayQuiet() {
        assertFalse(shouldPlay(enabled = true, ringerMode = AudioManager.RINGER_MODE_SILENT))
        assertFalse(shouldPlay(enabled = true, ringerMode = AudioManager.RINGER_MODE_VIBRATE))
    }

    @Test fun disabledStaysQuietInNormalMode() {
        assertFalse(shouldPlay(enabled = false, ringerMode = AudioManager.RINGER_MODE_NORMAL))
    }

    @Test fun everySoundHasAnAssetPath() {
        ShutterSound.entries.forEach { sound ->
            assertTrue(sound.asset.startsWith("sounds/shutter_"))
            assertTrue(sound.asset.endsWith(".wav"))
        }
    }
}
