package com.example.cameraxapp.core.render

/** Cycling order for the camera filter swipe. Pure Kotlin, usable from JVM unit tests. */
object FilterCycle {

    /**
     * Returns the id [delta] steps from [current] in [ids], wrapping at both ends.
     * Positive [delta] moves forward (left swipe), negative moves back (right swipe).
     * If [current] is not in [ids], the step starts from the first id going forward or the last id
     * going back. Returns [current] when [ids] is empty.
     */
    fun next(ids: List<String>, current: String, delta: Int): String {
        if (ids.isEmpty()) return current
        val index = ids.indexOf(current)
        if (index < 0) return if (delta < 0) ids.last() else ids.first()
        val size = ids.size
        val target = ((index + delta) % size + size) % size
        return ids[target]
    }
}
