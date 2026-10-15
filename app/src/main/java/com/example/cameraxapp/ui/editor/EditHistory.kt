package com.example.cameraxapp.ui.editor

/**
 * Undo/redo for a value that changes continuously. [update] changes the present value without
 * recording history; [commit] records the value from the last commit as one undo step. The UI
 * calls commit on slider release and after discrete actions, so a drag is one step, not hundreds.
 */
class EditHistory<T>(initial: T, private val capacity: Int = MAX_STEPS) {

    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()
    private var baseline: T = initial

    var present: T = initial
        private set

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    /** Changes the present value without an undo step. */
    fun update(value: T) {
        present = value
    }

    /** Records the change since the last commit. Returns false when nothing changed. */
    fun commit(): Boolean {
        if (present == baseline) return false
        pushPast(baseline)
        future.clear()
        baseline = present
        return true
    }

    /** Commits any pending change, then steps back. Returns the new present, or null at the start. */
    fun undo(): T? {
        commit()
        if (past.isEmpty()) return null
        future.addLast(present)
        present = past.removeLast()
        baseline = present
        return present
    }

    fun redo(): T? {
        if (future.isEmpty()) return null
        pushPast(present)
        present = future.removeLast()
        baseline = present
        return present
    }

    /** Sets [value] and records it as one undo step, so reset can itself be undone. */
    fun reset(value: T) {
        update(value)
        commit()
    }

    private fun pushPast(value: T) {
        past.addLast(value)
        if (past.size > capacity) past.removeFirst()
    }

    companion object {
        const val MAX_STEPS = 30
    }
}
