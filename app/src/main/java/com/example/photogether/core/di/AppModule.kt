package com.example.photogether.core.di

import com.example.photogether.core.analytics.Analytics
import com.example.photogether.core.analytics.NoOpAnalytics
import com.example.photogether.data.AppDatabase
import com.example.photogether.data.billing.BillingRepository
import com.example.photogether.data.repository.PhotoBoothRepository
import com.example.photogether.data.settings.CameraSettingsRepository
import com.example.photogether.data.repository.RecipeRepository
import java.io.File
import com.example.photogether.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.photogether.ui.viewmodel.PhotoBoothViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<Analytics> { NoOpAnalytics }
    single { BillingRepository(androidContext(), get()) }
    single { PhotoBoothRepository(androidContext()) }
    single { AppDatabase.getDatabase(androidApplication()) }
    single { RecipeRepository(get<AppDatabase>().recipeDao(), File(androidContext().filesDir, "recipes")) }
    single { CameraSettingsRepository.create(androidContext()) }
    single { com.example.photogether.data.prefs.FavoriteFiltersRepository.create(androidContext()) }
    single { com.example.photogether.data.prefs.OnboardingRepository.create(androidContext()) }
    single<com.example.photogether.data.prefs.OnboardingStatus> { get<com.example.photogether.data.prefs.OnboardingRepository>() }
    viewModel { com.example.photogether.ui.onboarding.OnboardingViewModel(get()) }
    single { com.example.photogether.data.moments.MomentsRepository(androidContext().contentResolver) }
    viewModel { com.example.photogether.ui.moments.MomentsViewModel(get()) }
    single { CreatePhotoBoothImageUseCase(get<BillingRepository>().isPremium, get()) }
    single { com.example.photogether.domain.usecase.GifExportUseCase(get<BillingRepository>().isPremium) }
    viewModel {
        PhotoBoothViewModel(
            repository = get(),
            application = get(),
            createPhotoBoothImageUseCase = get(),
            gifExportUseCase = get(),
            recipeRepository = get(),
            isPremium = get<BillingRepository>().isPremium,
            analytics = get(),
            catalog = get(),
        )
    }
    viewModel {
        com.example.photogether.ui.viewmodel.CameraViewModel(
            application = androidApplication(),
            recipeRepository = get(),
            settingsRepository = get(),
            analytics = get(),
            pendingLook = get(),
            catalog = get(),
        )
    }
    viewModel { com.example.photogether.ui.viewmodel.GalleryViewModel(get()) }
    viewModel { com.example.photogether.ui.viewmodel.SettingsViewModel(androidApplication()) }
    single { com.example.photogether.core.render.FilterCatalog(androidContext()) }
    single {
        com.example.photogether.data.colors.ColorStoryRepository(
            assets = androidContext().assets,
            renderer = com.example.photogether.core.render.EditPipeline(get()),
            cacheRoot = File(androidContext().filesDir, "color_stories"),
        )
    }
    single { com.example.photogether.core.colors.PendingColorLook() }
    viewModel {
        com.example.photogether.ui.colors.ColorStoriesViewModel(
            repository = get(),
            catalog = get(),
            cameraSettings = get(),
            pendingLook = get(),
        )
    }
    factory<com.example.photogether.ui.editor.EditorImageIo> {
        com.example.photogether.ui.editor.AndroidEditorImageIo(androidContext())
    }
    viewModel {
        com.example.photogether.ui.editor.EditorViewModel(
            io = get(),
            renderer = com.example.photogether.core.render.EditPipeline(get()),
            filters = get<com.example.photogether.core.render.FilterCatalog>().filters,
            recipeRepository = get(),
            isPremium = get<com.example.photogether.data.billing.BillingRepository>().isPremium,
            analytics = get(),
            favorites = get<com.example.photogether.data.prefs.FavoriteFiltersRepository>().favorites,
            onToggleFavorite = { id ->
                get<com.example.photogether.data.prefs.FavoriteFiltersRepository>().toggle(id)
            },
        )
    }
}
