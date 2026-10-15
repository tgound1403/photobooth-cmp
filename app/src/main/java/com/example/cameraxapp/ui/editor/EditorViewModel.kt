package com.example.cameraxapp.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.cameraxapp.core.render.CropParams
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.EditRenderer
import com.example.cameraxapp.core.render.EditSnapshot
import com.example.cameraxapp.core.render.FrameSpec
import com.example.cameraxapp.core.render.OverlaySpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the edit being made to one photo. Slider drags call [setParams] (live preview only) and
 * then [commitParams] on release, so each drag is one undo step. Discrete actions call the
 * matching setter, which commits immediately.
 */
class EditorViewModel(
    private val io: EditorImageIo,
    private val renderer: EditRenderer,
    uiDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + uiDispatcher)
    private val history = EditHistory(EditSnapshot())

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emits the content uri of each saved image. */
    val saved: SharedFlow<String> = _saved.asSharedFlow()

    private var source: Bitmap? = null
    private var density = 1f
    private var backgroundUri: String? = null
    private var background: Bitmap? = null
    private var loadStarted = false

    // Latest-wins: many slider frames collapse into one render.
    private val renderRequests = Channel<Unit>(Channel.CONFLATED)
    private val thumbRequests = Channel<String>(Channel.UNLIMITED)
    private val pendingThumbs = HashSet<String>()
    private val thumbCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
            size > THUMB_CACHE_SIZE
    }

    /** Decodes the photo and starts the render and thumbnail loops. Later calls are ignored. */
    fun load(uri: String, density: Float) {
        if (loadStarted) return
        loadStarted = true
        this.density = density
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { io.decode(uri, SOURCE_MAX_SIDE) }
            if (bitmap == null) {
                loadStarted = false
                _state.update { it.copy(isLoading = false, error = "Could not open image") }
                return@launch
            }
            source = bitmap
            _state.update { it.copy(isLoading = false, sourceLoaded = true) }
            startRenderLoop()
            startThumbnailLoop()
            requestRender()
        }
    }

    /** Live slider value: updates the preview, not the undo history. */
    fun setParams(params: EditParams) = liveEdit { it.copy(params = params.clamped()) }

    /** Ends a slider drag: records the change since the last commit as one undo step. */
    fun commitParams() = commitEdit()

    fun selectFilter(filterId: String) = discreteEdit { it.copy(params = it.params.copy(filterId = filterId)) }

    fun setCrop(crop: CropParams) = discreteEdit { it.copy(params = it.params.copy(crop = crop)) }

    fun setOverlays(overlays: OverlaySpec) = discreteEdit { it.copy(overlays = overlays) }

    fun setFrame(frame: FrameSpec) = discreteEdit { it.copy(frame = frame) }

    fun undo() {
        if (history.undo() != null) publish()
    }

    fun redo() {
        if (history.redo() != null) publish()
    }

    /** Restores the unedited photo. Undoable like any other change. */
    fun reset() {
        history.reset(EditSnapshot())
        publish()
    }

    /** Press-and-hold: while true the screen shows the original. Not part of undo. */
    fun setShowOriginal(show: Boolean) {
        _state.update { it.copy(showOriginal = show) }
    }

    /** Queues a small preview of [filterId] for the filter strip. Duplicates are ignored. */
    fun requestThumbnail(filterId: String) {
        if (thumbCache.containsKey(filterId) || !pendingThumbs.add(filterId)) return
        thumbRequests.trySend(filterId)
    }

    /** Renders the edit at full size and saves it as a new JPEG. */
    fun save() {
        if (_state.value.isSaving) return
        val src = source ?: return
        scope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val snapshot = history.present
            val bg = backgroundFor(snapshot.frame)
            val full = renderer.render(src, snapshot, bg, density, maxSize = null)
            val uri = withContext(Dispatchers.IO) { io.saveJpeg(full) }
            _state.update { it.copy(isSaving = false, error = if (uri == null) "Could not save image" else null) }
            if (uri != null) _saved.emit(uri)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        scope.cancel()
        renderer.close()
    }

    private fun liveEdit(transform: (EditSnapshot) -> EditSnapshot) {
        history.update(transform(history.present))
        publish()
        requestRender()
    }

    private fun commitEdit() {
        if (history.commit()) publish()
    }

    private fun discreteEdit(transform: (EditSnapshot) -> EditSnapshot) {
        liveEdit(transform)
        commitEdit()
    }

    private fun publish() {
        _state.update {
            it.copy(
                snapshot = history.present,
                canUndo = history.canUndo,
                canRedo = history.canRedo,
            )
        }
    }

    private fun requestRender() {
        renderRequests.trySend(Unit)
    }

    private fun startRenderLoop() {
        scope.launch {
            for (ignored in renderRequests) {
                val src = source ?: continue
                val snapshot = history.present
                val bg = backgroundFor(snapshot.frame)
                val out = renderer.render(src, snapshot, bg, density, PREVIEW_MAX_SIDE)
                _state.update { it.copy(preview = out) }
            }
        }
    }

    private fun startThumbnailLoop() {
        scope.launch {
            for (filterId in thumbRequests) {
                val src = source
                if (src != null) {
                    val thumb = renderer.render(
                        src,
                        EditSnapshot(params = EditParams(filterId = filterId)),
                        null,
                        density,
                        THUMB_MAX_SIDE,
                    )
                    thumbCache[filterId] = thumb
                    _state.update { it.copy(thumbnails = thumbCache.toMap()) }
                }
                pendingThumbs.remove(filterId)
            }
        }
    }

    private suspend fun backgroundFor(frame: FrameSpec): Bitmap? {
        val uri = frame.backgroundUri ?: return null
        if (uri != backgroundUri) {
            backgroundUri = uri
            background = withContext(Dispatchers.IO) { io.decode(uri, SOURCE_MAX_SIDE) }
        }
        return background
    }

    companion object {
        const val PREVIEW_MAX_SIDE = 1280
        const val SOURCE_MAX_SIDE = 2560
        const val THUMB_MAX_SIDE = 160
        const val THUMB_CACHE_SIZE = 40
    }
}
