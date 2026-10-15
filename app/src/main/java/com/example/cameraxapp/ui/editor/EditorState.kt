package com.example.cameraxapp.ui.editor

import android.graphics.Bitmap
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.EditSnapshot

/** Everything the editor screen shows. Bitmaps are immutable once produced by the pipeline. */
/** A change to [EditParams], applied to the latest value. */
typealias ParamsEdit = (EditParams) -> EditParams

data class EditorState(
    val isLoading: Boolean = false,
    val sourceLoaded: Boolean = false,
    /** The decoded photo before any edit, shown while the press-and-hold control is held. */
    val original: Bitmap? = null,
    val preview: Bitmap? = null,
    val snapshot: EditSnapshot = EditSnapshot(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** True while the user holds the press-and-hold control; the screen then shows the original. */
    val showOriginal: Boolean = false,
    /** Filter id to small preview, for the filter strip. */
    val thumbnails: Map<String, Bitmap> = emptyMap(),
    val isSaving: Boolean = false,
    /** True when a save or recipe save needs Premium and the paywall is open. */
    val showPaywall: Boolean = false,
    val error: String? = null,
)
