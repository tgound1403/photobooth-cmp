package com.example.photogether

import android.app.Application
import com.example.photogether.core.di.appModule
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.photogether.data.billing.BillingRepository
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
        // Play connects on the first foreground (onStart below), so nothing billing-related runs
        // on the Application's main-thread startup path. connect() is a no-op when already connected,
        // and it reconnects if the Play connection dropped. Premium can change outside the app
        // (cancel, renew, refund), so re-check each time the app returns to the foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val billing = koin.koin.get<BillingRepository>()
                billing.connect()
                billing.refreshPurchases()
            }
        })
    }
}