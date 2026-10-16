package com.example.photogether.ui.camera

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.exp

/**
 * How far the user has turned the picker's 3D view by swiping. The camera body and the polaroid
 * orbits both read it, so one swipe turns the body and speeds up the orbits together.
 *
 * Horizontal swipes turn the body around its vertical axis and push the cards along their orbits.
 * Vertical swipes tilt the body and open or close the orbit rings, which changes the axis the cards
 * travel around. The values are read inside layout and draw lambdas, so a swipe does not recompose.
 */
@Stable
class PickerSpin {
    /** Extra yaw of the body in degrees, added to its automatic sway. */
    var bodyYaw by mutableFloatStateOf(0f)
        private set

    /** Pitch of the body in degrees. Positive tips the front down. */
    var bodyPitch by mutableFloatStateOf(0f)
        private set

    /** Extra position of every card on its ring, in laps. Positive moves cards toward the left. */
    var lapOffset by mutableFloatStateOf(0f)
        private set

    /** Extra ring elevation in degrees, added to each orbit's own elevation. */
    var tiltOffset by mutableFloatStateOf(0f)
        private set

    /** Applies a finger movement of [dx], [dy] pixels. */
    fun drag(dx: Float, dy: Float) {
        bodyYaw += dx * YAW_PER_PX
        bodyPitch = (bodyPitch + dy * PITCH_PER_PX).coerceIn(-MAX_PITCH, MAX_PITCH)
        lapOffset += dx * LAP_PER_PX
        tiltOffset = (tiltOffset + dy * TILT_PER_PX).coerceIn(-MAX_TILT, MAX_TILT)
    }

    /** Keeps turning after the finger lifts at [vx], [vy] pixels per second, slowing down by friction. */
    suspend fun fling(vx: Float, vy: Float) {
        var x = vx
        var y = vy
        var last = withFrameNanos { it }
        while (abs(x) > MIN_FLING_SPEED || abs(y) > MIN_FLING_SPEED) {
            val now = withFrameNanos { it }
            val dt = (now - last) / NANOS_PER_SECOND
            last = now
            drag(x * dt, y * dt)
            val keep = exp(-FLING_FRICTION * dt)
            x *= keep
            y *= keep
        }
    }

    private companion object {
        const val YAW_PER_PX = 0.5f
        const val PITCH_PER_PX = 0.3f
        const val MAX_PITCH = 35f

        /** Negative: a swipe to the right moves the front of each ring to the right. */
        const val LAP_PER_PX = -0.0015f
        const val TILT_PER_PX = 0.1f
        const val MAX_TILT = 25f

        const val MIN_FLING_SPEED = 20f
        const val FLING_FRICTION = 2.5f
        const val NANOS_PER_SECOND = 1_000_000_000f
    }
}
