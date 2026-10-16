package com.example.cameraxapp.core.di

import com.example.cameraxapp.core.analytics.Analytics
import com.example.cameraxapp.core.analytics.NoOpAnalytics
import com.example.cameraxapp.data.AppDatabase
import com.example.cameraxapp.data.billing.BillingRepository
import com.example.cameraxapp.data.repository.PhotoBoothRepository
import com.example.cameraxapp.data.settings.CameraSettingsRepository
import com.example.cameraxapp.data.repository.RecipeRepository
import java.io.File
import com.example.cameraxapp.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
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
    single { CreatePhotoBoothImageUseCase(get<BillingRepository>().isPremium) }
    single { com.example.cameraxapp.domain.usecase.GifExportUseCase(get<BillingRepository>().isPremium) }
    viewModel {
        PhotoBoothViewModel(
            repository = get(),
            application = get(),
            createPhotoBoothImageUseCase = get(),
            gifExportUseCase = get(),
            recipeRepository = get(),
            isPremium = get<BillingRepository>().isPremium,
            analytics = get(),
        )
    }
    viewModel {
        com.example.cameraxapp.ui.viewmodel.CameraViewModel(
            application = androidApplication(),
            recipeRepository = get(),
            settingsRepository = get(),
            analytics = get(),
        )
    }
    viewModel { com.example.cameraxapp.ui.viewmodel.GalleryViewModel(get()) }
    single { com.example.cameraxapp.core.render.FilterCatalog(androidContext()) }
    factory<com.example.cameraxapp.ui.editor.EditorImageIo> {
        com.example.cameraxapp.ui.editor.AndroidEditorImageIo(androidContext())
    }
    viewModel {
        com.example.cameraxapp.ui.editor.EditorViewModel(
            io = get(),
            renderer = com.example.cameraxapp.core.render.EditPipeline(get()),
            filters = get<com.example.cameraxapp.core.render.FilterCatalog>().filters,
            recipeRepository = get(),
            isPremium = get<com.example.cameraxapp.data.billing.BillingRepository>().isPremium,
            analytics = get(),
        )
    }
}
