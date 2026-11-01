package com.example.photogether

import android.app.Application
import com.example.photogether.core.di.appModule
import com.example.photogether.core.render.FilterCatalog
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
        // Parse the 114 KB filter catalog off the main thread. PhotoBoothViewModel reads it while composing, and
        // on a slow device that parse froze the first frames long enough for an ANR. The lazy is synchronized,
        // so a reader that arrives early waits only for the rest of this parse.
        Thread({ koin.koin.get<FilterCatalog>().filters }, "catalog-preload").start()
    }
}