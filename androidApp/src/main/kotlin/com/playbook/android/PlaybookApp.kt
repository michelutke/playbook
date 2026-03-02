package com.playbook.android

import android.app.Application
import com.playbook.android.di.uiModule
import com.playbook.data.network.ApiConfig
import com.playbook.di.androidPlatformModule
import com.playbook.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlaybookApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PlaybookApp)
            modules(
                androidPlatformModule(this@PlaybookApp),
                sharedModule(
                    ApiConfig(
                        baseUrl = "http://10.0.2.2:8080", // TODO: inject via BuildConfig
                        authTokenProvider = { null },      // TODO: inject from auth module
                    )
                ),
                uiModule,
            )
        }
    }
}
