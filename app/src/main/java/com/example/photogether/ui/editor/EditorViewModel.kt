package com.example.photogether.ui.editor

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.photogether.core.render.CropMath
import com.example.photogether.core.render.CropParams
import com.example.photogether.core.analytics.Analytics
import com.example.photogether.core.analytics.AnalyticsEvent
import com.example.photogether.core.analytics.NoOpAnalytics
import com.example.photogether.core.render.EditParams
import com.example.photogether.core.render.EditRenderer
import com.example.photogether.core.render.EditSnapshot
import com.example.photogether.core.render.FilterInfo
import com.example.photogether.core.render.FrameSpec
import com.example.photogether.core.render.OverlaySpec
import com.example.photogether.core.render.PolaroidGesture
import com.example.photogether.core.render.PolaroidMode
import com.example.photogether.core.render.Recipe
import com.example.photogether.core.render.RecipeCode
import com.example.photogether.core.render.RecipeCodeResult
import com.example.photogether.data.billing.requiresPaywall
import com.example.photogether.data.repository.RecipeRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
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
    /** Filter list for the filter strip, supplied by DI. */
    val filters: List<FilterInfo> = emptyList(),
    private val recipeRepository: RecipeRepository,
    /** Premium entitlement. Saving a premium filter while this is false opens the paywall. */
    val isPremium: StateFlow<Boolean> = MutableStateFlow(false),
    private val analytics: Analytics = NoOpAnalytics,
    uiDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    /** Ids of favorited filters. Empty unless the app supplies the favorites store. */
    favorites: Flow<Set<String>> = emptyFlow(),
    /** Stores a favorite toggle for a filter id. Does nothing unless the app supplies it. */
    private val onToggleFavorite: suspend (String) -> Unit = {},
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + uiDispatcher)
    private val history = EditHistory(EditSnapshot())

    /** Saved recipes, newest first. */
    val recipes: StateFlow<List<Recipe>> =
        recipeRepository.observeRecipes().stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Short messages for the snackbar: a recipe was saved or imported, or why an import failed. */
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        scope.launch {
            favorites.collect { ids -> _state.update { it.copy(favorites = ids) } }
        }
    }

    private val _saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emits the content uri of each saved image. */
    val saved: SharedFlow<String> = _saved.asSharedFlow()

    private var source: Bitmap? = null
    /** True while a finger is on the photo. The preview then renders smaller; see [setFrameInteracting]. */
    @Volatile private var interacting = false
    private var settleJob: Job? = null
    private var density = 1f
    private var backgroundUri: String? = null
    private var background: Bitmap? = null
    private var loadStarted = false

    // Latest-wins: many slider frames collapse into one render.
    private val renderRequests = Channel<Unit>(Channel.CONFLATED)
    // Bounded and drop-oldest: when the strip scrolls fast, the stale requests are discarded.
    // A dropped id is removed from pendingThumbs so it can be requested again later.
    private val pendingThumbs: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val thumbRequests = Channel<String>(
        capacity = THUMB_QUEUE_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { pendingThumbs.remove(it) },
    )
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
            _state.update { it.copy(isLoading = false, sourceLoaded = true, original = bitmap) }
            startRenderLoop()
            startThumbnailLoop()
            requestRender()
        }
    }

    /** Live slider value: updates the preview, not the undo history. */
    fun setParams(params: EditParams) = updateParams { params }

    /** Live slider change applied to the latest params, so fast drag events are never lost. */
    fun updateParams(transform: ParamsEdit) = liveEdit { it.copy(params = transform(it.params).clamped()) }

    /** Ends a slider drag: records the change since the last commit as one undo step. */
    fun commitParams() = commitEdit()

    fun selectFilter(filterId: String) {
        discreteEdit { it.copy(params = it.params.copy(filterId = filterId)) }
        analytics.log(AnalyticsEvent.FILTER_APPLIED, mapOf("filter_id" to filterId))
    }

    fun setCrop(crop: CropParams) = discreteEdit { it.copy(params = it.params.copy(crop = crop)) }

    fun setOverlays(overlays: OverlaySpec) = discreteEdit { it.copy(overlays = overlays) }

    fun setFrame(frame: FrameSpec) = discreteEdit { it.copy(frame = frame) }

    /** Frame change for the live preview only (caption typing, drags). Pair with [commitFrame]. */
    fun updateFrameLive(transform: (FrameSpec) -> FrameSpec) = liveEdit { it.copy(frame = transform(it.frame)) }

    /** Ends a frame edit: records the change since the last commit as one undo step. */
    fun commitFrame() = commitEdit()

    /**
     * True while a finger is on the photo. The preview renders at [INTERACT_MAX_SIDE] then, and at
     * full preview size once released.
     */
    fun setFrameInteracting(active: Boolean) {
        if (interacting == active) return
        interacting = active
        if (!active) requestRender()
    }

    /**
     * Pan and pinch of the photo inside a FILL Polaroid frame. [panPxX], [panPxY] and [windowSidePx]
     * are in screen pixels; [zoomBy] is the pinch factor for this event. Live only: call
     * [commitFrame] when the gesture ends. Does nothing without a source photo or outside FILL.
     */
    fun dragPlacement(panPxX: Float, panPxY: Float, zoomBy: Float, windowSidePx: Float) {
        val src = source ?: return
        val present = history.present
        if (present.frame.polaroid != PolaroidMode.FILL) return
        val (photoW, photoH) = croppedSize(src, present.params.crop)
        updateFrameLive { frame ->
            frame.copy(
                placement = PolaroidGesture.apply(
                    frame.placement,
                    panPxX,
                    panPxY,
                    zoomBy,
                    windowSidePx,
                    photoW,
                    photoH,
                ),
            )
        }
    }

    /** Adds [filterId] to the favorites, or removes it. The result reaches [EditorState.favorites]. */
    fun toggleFavorite(filterId: String) {
        scope.launch { onToggleFavorite(filterId) }
    }

    fun dismissShareSheet() {
        _state.update { it.copy(showShareSheet = false) }
    }

    /** Records the saved image and opens the share sheet. [lastSavedUri] stays after dismiss. */
    internal fun onImageSaved(uri: String) {
        _state.update { it.copy(lastSavedUri = uri, showShareSheet = true) }
    }

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
        if (_state.value.thumbnails.containsKey(filterId) || !pendingThumbs.add(filterId)) return
        thumbRequests.trySend(filterId)
    }

    /** Renders the edit at full size and saves it as a new JPEG. */
    fun save() {
        if (_state.value.isSaving) return
        if (requiresPaywall(history.present.params.filterId, filters, isPremium.value)) {
            openPaywall()
            return
        }
        val src = source ?: return
        scope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val uri = try {
                exportAndSave(src)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Save failed", e)
                null
            }
            _state.update { it.copy(isSaving = false, error = if (uri == null) "Could not save image" else null) }
            if (uri != null) {
                analytics.log(AnalyticsEvent.IMAGE_SAVED)
                _saved.emit(uri)
                onImageSaved(uri)
            }
        }
    }

    /**
     * Renders the current edit at the export size and writes it to the gallery. If the full-size render runs
     * out of memory, it retries once at [EXPORT_FALLBACK_SIDE].
     */
    private suspend fun exportAndSave(src: Bitmap): String? {
        val snapshot = history.present
        val bg = backgroundFor(snapshot.frame)
        val full = try {
            renderer.render(src, snapshot, bg, density, io.exportMaxSide)
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Export ran out of memory, retrying at $EXPORT_FALLBACK_SIDE", e)
            renderer.render(src, snapshot, bg, density, EXPORT_FALLBACK_SIDE)
        }
        return withContext(Dispatchers.IO) { io.saveJpeg(full) }
    }

    /** Applies a recipe's edit values as one undoable step. Overlays and frame are kept. */
    fun applyRecipe(params: EditParams) = discreteEdit { it.copy(params = params.clamped()) }

    /** Saves the current edit values, crop included, as a new recipe with a rendered thumbnail. */
    fun saveRecipe() {
        val src = source
        val params = history.present.params
        if (requiresPaywall(params.filterId, filters, isPremium.value)) {
            openPaywall()
            return
        }
        val name = "Recipe ${recipes.value.size + 1}"
        scope.launch {
            val thumb = src?.let { renderer.render(it, EditSnapshot(params = params), null, density, THUMB_MAX_SIDE) }
            recipeRepository.saveRecipe(name, params, thumb)
            _notices.tryEmit("Recipe saved")
        }
    }

    /** Imports a pasted share code into the library. Nothing is applied until the user taps it. */
    fun importRecipeCode(code: String) {
        when (val result = RecipeCode.decode(code)) {
            is RecipeCodeResult.Error -> _notices.tryEmit(result.message)
            is RecipeCodeResult.Ok -> scope.launch {
                recipeRepository.saveRecipe("Imported ${recipes.value.size + 1}", result.params, null)
                _notices.tryEmit("Recipe imported")
            }
        }
    }

    /** Closes the paywall. Nothing is saved until the user taps Save again. */
    fun dismissPaywall() {
        _state.update { it.copy(showPaywall = false) }
    }

    private fun openPaywall() {
        _state.update { it.copy(showPaywall = true) }
        analytics.log(AnalyticsEvent.PAYWALL_SHOWN, mapOf("trigger" to "save"))
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
        markSliderActive()
        requestRender()
    }

    /**
     * Slider drags render at [INTERACT_MAX_SIDE]. Once the edits stop for [SETTLE_MS], the preview
     * renders again at full preview size.
     */
    private fun markSliderActive() {
        interacting = true
        settleJob?.cancel()
        settleJob = scope.launch {
            delay(SETTLE_MS)
            interacting = false
            requestRender()
        }
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
                val maxSide = if (interacting) INTERACT_MAX_SIDE else PREVIEW_MAX_SIDE
                // A failed render keeps the last preview; the loop must survive to render the next edit.
                val out = renderOrNull(src, snapshot, bg, maxSide) ?: continue
                _state.update { it.copy(preview = out) }
            }
        }
    }

    /** Size of the photo after the crop (rotation and aspect), in source pixels. */
    private fun croppedSize(src: Bitmap, crop: CropParams): Pair<Int, Int> {
        val c = crop.normalized()
        val (w, h) = CropMath.rotatedSize(src.width, src.height, c.quarterTurns)
        val rect = CropMath.cropRect(w, h, c.aspect)
        return rect.width to rect.height
    }

    private fun startThumbnailLoop() {
        scope.launch {
            for (filterId in thumbRequests) {
                val src = source
                if (src != null) {
                    val thumb = renderOrNull(src, EditSnapshot(params = EditParams(filterId = filterId)), null, THUMB_MAX_SIDE)
                    if (thumb != null) {
                        thumbCache[filterId] = thumb
                        _state.update { it.copy(thumbnails = thumbCache.toMap()) }
                    }
                }
                pendingThumbs.remove(filterId)
            }
        }
    }

    /** Renders one frame. A failure is logged and returns null, so the calling loop keeps running. */
    private suspend fun renderOrNull(src: Bitmap, snapshot: EditSnapshot, bg: Bitmap?, maxSide: Int?): Bitmap? =
        try {
            renderer.render(src, snapshot, bg, density, maxSide)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Preview render ran out of memory at maxSide=$maxSide", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Preview render failed", e)
            null
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
        /** Preview size while a finger is on the photo, so drags stay smooth. */
        const val INTERACT_MAX_SIDE = 720
        /** Export size used when the full-size render runs out of memory. */
        const val EXPORT_FALLBACK_SIDE = 2048
        private const val TAG = "EditorViewModel"
        const val SOURCE_MAX_SIDE = 2560
        const val THUMB_MAX_SIDE = 160
        const val THUMB_CACHE_SIZE = 40
        const val THUMB_QUEUE_SIZE = 24
        /** Quiet time after the last slider edit before the preview is rendered at full size again. */
        const val SETTLE_MS = 250L
    }
}
