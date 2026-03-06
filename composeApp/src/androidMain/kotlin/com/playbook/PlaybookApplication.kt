package com.playbook

import android.app.Application
import com.playbook.android.BuildConfig
import com.playbook.preferences.UserPreferences
import com.playbook.push.OneSignalInitializer
import com.playbook.push.PushTokenManager
import com.playbook.data.network.ApiConfig
import com.playbook.di.androidComposeModule
import com.playbook.di.androidPlatformModule
import com.playbook.di.sharedModule
import com.playbook.di.uiModule
import com.playbook.repository.PushTokenRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlaybookApplication : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()
        val userPreferences = UserPreferences(this)
        OneSignalInitializer.init(this, BuildConfig.ONESIGNAL_APP_ID)
        startKoin {
            androidContext(this@PlaybookApplication)
            modules(
                androidPlatformModule(this@PlaybookApplication),
                sharedModule(
                    ApiConfig(
                        baseUrl = BuildConfig.BACKEND_BASE_URL,
                        authTokenProvider = { runBlocking { userPreferences.getToken() } },
                    )
                ),
                androidComposeModule,
                uiModule,
            )
        }
        val tokenRepo: PushTokenRepository by inject()
        @Suppress("OPT_IN_USAGE")
        PushTokenManager.observe(GlobalScope, tokenRepo)
    }
}
