package com.example.cameraxapp.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.cameraxapp.core.render.CropParams
import com.example.cameraxapp.core.analytics.Analytics
import com.example.cameraxapp.core.analytics.AnalyticsEvent
import com.example.cameraxapp.core.analytics.NoOpAnalytics
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.EditRenderer
import com.example.cameraxapp.core.render.EditSnapshot
import com.example.cameraxapp.core.render.FilterInfo
import com.example.cameraxapp.core.render.FrameSpec
import com.example.cameraxapp.core.render.OverlaySpec
import com.example.cameraxapp.core.render.Recipe
import com.example.cameraxapp.core.render.RecipeCode
import com.example.cameraxapp.core.render.RecipeCodeResult
import com.example.cameraxapp.data.billing.requiresPaywall
import com.example.cameraxapp.data.repository.RecipeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
        if (requiresPaywall(history.present.params.filterId, filters, isPremium.value)) {
            openPaywall()
            return
        }
        val src = source ?: return
        scope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val snapshot = history.present
            val bg = backgroundFor(snapshot.frame)
            val full = renderer.render(src, snapshot, bg, density, maxSize = null)
            val uri = withContext(Dispatchers.IO) { io.saveJpeg(full) }
            _state.update { it.copy(isSaving = false, error = if (uri == null) "Could not save image" else null) }
            if (uri != null) {
                analytics.log(AnalyticsEvent.IMAGE_SAVED)
                _saved.emit(uri)
            }
        }
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
