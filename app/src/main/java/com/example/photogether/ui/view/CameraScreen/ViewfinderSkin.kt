package com.example.photogether.ui.view.CameraScreen

import androidx.compose.ui.graphics.Color
import com.example.photogether.core.render.ViewfinderSkin

/** Shutter fill for the skin, or [Color.Unspecified] to keep the default. */
val ViewfinderSkin.shutterColor: Color
    get() = if (shutterTint == 0L) Color.Unspecified else Color(shutterTint)

/** Shutter ring colour, or [Color.Unspecified] for no ring. */
val ViewfinderSkin.shutterRingColor: Color
    get() = if (shutterRing == 0L) Color.Unspecified else Color(shutterRing)
