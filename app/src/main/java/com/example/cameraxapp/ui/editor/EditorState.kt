package com.example.cameraxapp.ui.editor

import android.graphics.Bitmap
import com.example.cameraxapp.core.render.EditSnapshot

/** Everything the editor screen shows. Bitmaps are immutable once produced by the pipeline. */
data class EditorState(
    val isLoading: Boolean = false,
    val sourceLoaded: Boolean = false,
    val preview: Bitmap? = null,
    val snapshot: EditSnapshot = EditSnapshot(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** True while the user holds the press-and-hold control; the screen then shows the original. */
    val showOriginal: Boolean = false,
    /** Filter id to small preview, for the filter strip. */
    val thumbnails: Map<String, Bitmap> = emptyMap(),
    val isSaving: Boolean = false,
    val error: String? = null,
)
