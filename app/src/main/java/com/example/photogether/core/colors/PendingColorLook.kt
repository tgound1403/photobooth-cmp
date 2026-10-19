package com.example.photogether.core.colors

import com.example.photogether.core.render.EditParams

/**
 * Carries a colour story's look from its detail page to the camera screen. The camera takes it
 * once, when its view model is created, so the first frame already shows the look.
 */
class PendingColorLook {
    private var params: EditParams? = null

    fun set(look: EditParams) {
        params = look
    }

    /** Returns the pending look and clears it, so it applies only once. */
    fun take(): EditParams? = params.also { params = null }
}
