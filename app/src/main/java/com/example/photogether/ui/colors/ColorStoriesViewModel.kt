package com.example.photogether.ui.colors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.core.colors.ColorStory
import com.example.photogether.core.colors.PendingColorLook
import com.example.photogether.core.locale.AppLocale
import com.example.photogether.core.render.CameraProfiles
import com.example.photogether.core.render.FilterCatalog
import com.example.photogether.data.colors.ColorPhoto
import com.example.photogether.data.colors.ColorStoryRepository
import com.example.photogether.data.colors.SampleCredit
import com.example.photogether.data.settings.CameraSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Photos of one story on its detail page, and the name of its filter. */
data class ColorStoryDetailState(
    val photos: List<ColorPhoto> = emptyList(),
    val filterName: String = "",
    val loading: Boolean = true,
)

class ColorStoriesViewModel(
    private val repository: ColorStoryRepository,
    private val catalog: FilterCatalog,
    private val cameraSettings: CameraSettingsRepository,
    private val pendingLook: PendingColorLook,
) : ViewModel() {

    private val _covers = MutableStateFlow<Map<String, ColorPhoto>>(emptyMap())
    /** Story id to its graded cover photo. Stories without a cover yet are missing. */
    val covers: StateFlow<Map<String, ColorPhoto>> = _covers.asStateFlow()

    private val _detail = MutableStateFlow(ColorStoryDetailState())
    val detail: StateFlow<ColorStoryDetailState> = _detail.asStateFlow()

    private val _credits = MutableStateFlow<List<SampleCredit>>(emptyList())
    /** Attribution of every bundled sample, for the credits page. */
    val credits: StateFlow<List<SampleCredit>> = _credits.asStateFlow()

    private var coversStarted = false

    /** Renders the cover of every story, one after another, so tiles fill in as they finish. */
    fun loadCovers(stories: List<ColorStory> = ColorStories.ALL) {
        if (coversStarted) return
        coversStarted = true
        viewModelScope.launch {
            stories.forEach { story ->
                val cover = repository.cover(story, AppLocale.current()) ?: return@forEach
                _covers.update { it + (story.id to cover) }
            }
        }
    }

    fun loadDetail(story: ColorStory) {
        viewModelScope.launch {
            val name = catalog.get(story.filterId)?.name.orEmpty()
            _detail.value = ColorStoryDetailState(filterName = name, loading = true)
            _detail.value = ColorStoryDetailState(
                photos = repository.photos(story, AppLocale.current()),
                filterName = name,
                loading = false,
            )
        }
    }

    /**
     * Opens the camera with [story]'s look. The camera body that owns the filter is saved first, so
     * the camera screen does not switch to another body's first filter on open.
     */
    fun tryNow(story: ColorStory, onReady: () -> Unit) {
        viewModelScope.launch {
            CameraProfiles.forFilter(story.filterId)?.let { cameraSettings.setCameraId(it.id) }
            pendingLook.set(ColorStoryRepository.look(story.filterId))
            onReady()
        }
    }

    fun loadCredits() {
        viewModelScope.launch { _credits.value = repository.credits().photos }
    }
}
