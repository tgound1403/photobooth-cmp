package com.example.cameraxapp.core.di

import com.example.cameraxapp.data.AppDatabase
import com.example.cameraxapp.data.repository.PhotoBoothRepository
import com.example.cameraxapp.data.repository.ThemeRepository
import com.example.cameraxapp.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import com.example.cameraxapp.ui.viewmodel.ThemeViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { PhotoBoothRepository(androidContext()) }
    single { AppDatabase.getDatabase(androidApplication()) }
    single { CreatePhotoBoothImageUseCase() }
    single { com.example.cameraxapp.domain.usecase.GifExportUseCase() }
    viewModel { PhotoBoothViewModel(get(), get(), get(), get()) }
    single { ThemeRepository(androidContext()) }
    viewModel { ThemeViewModel(get()) }
    viewModel { com.example.cameraxapp.ui.viewmodel.GalleryViewModel(get()) }
}
