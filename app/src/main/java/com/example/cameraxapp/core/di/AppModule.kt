package com.example.cameraxapp.core.di

import com.example.cameraxapp.data.AppDatabase
import com.example.cameraxapp.data.repository.PhotoBoothRepository
import com.example.cameraxapp.domain.usecase.CreatePhotoBoothImageUseCase
import com.example.cameraxapp.domain.usecase.SaveImageUseCase
import com.example.cameraxapp.ui.viewmodel.PhotoBoothViewModel
import com.example.cameraxapp.ui.viewmodel.BackgroundSelectorViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel
import kotlin.math.sin

val appModule = module {
    single { PhotoBoothRepository(androidContext()) }
    single { AppDatabase.getDatabase(androidApplication()) }
    single { SaveImageUseCase() }
    single { CreatePhotoBoothImageUseCase() }
    viewModel { PhotoBoothViewModel(get(), androidApplication(), get(), get()) }
    viewModel { BackgroundSelectorViewModel() }
} 