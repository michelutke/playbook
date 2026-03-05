package com.playbook.android

import android.app.Application
import com.playbook.android.preferences.UserPreferences
import com.playbook.android.push.OneSignalInitializer
import com.playbook.android.push.PushTokenManager
import com.playbook.data.network.ApiConfig
import com.playbook.di.androidComposeModule
import com.playbook.di.androidPlatformModule
import com.playbook.di.sharedModule
import com.playbook.di.uiModule as composeUiModule
import com.playbook.repository.PushTokenRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

class PlaybookApp : Application(), KoinComponent {
    private lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
        OneSignalInitializer.init(this, BuildConfig.ONESIGNAL_APP_ID)
        startKoin {
            androidContext(this@PlaybookApp)
            modules(
                module { single { userPreferences } },
                androidPlatformModule(this@PlaybookApp),
                sharedModule(
                    ApiConfig(
                        baseUrl = BuildConfig.BACKEND_BASE_URL,
                        authTokenProvider = { runBlocking { userPreferences.getToken() } },
                    )
                ),
                androidComposeModule,
                composeUiModule,
            )
        }
        val tokenRepo: PushTokenRepository by inject()
        @Suppress("OPT_IN_USAGE")
        PushTokenManager.observe(GlobalScope, tokenRepo)
    }
}
