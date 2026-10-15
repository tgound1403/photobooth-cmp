package com.example.cameraxapp

import android.app.Application
import com.example.cameraxapp.core.di.appModule
import com.example.cameraxapp.data.billing.BillingRepository
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
        koin.koin.get<BillingRepository>().connect()
    }
}