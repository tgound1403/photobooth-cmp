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
        val billing = koin.koin.get<BillingRepository>()
        billing.connect()
        // Premium can change outside the app (cancel, renew, refund), so re-check each time the app
        // returns to the foreground. connect() is a no-op when already connected, and it reconnects
        // if the Play connection dropped.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                billing.connect()
                billing.refreshPurchases()
            }
        })
    }
}