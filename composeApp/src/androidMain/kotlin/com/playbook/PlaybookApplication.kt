package com.playbook

import android.app.Application
import com.playbook.compose.BuildConfig
import com.playbook.di.sharedModule
import com.playbook.di.uiModule
import com.appswithlove.updraft.Settings
import com.appswithlove.updraft.Updraft
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlaybookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@PlaybookApplication)
            modules(sharedModule, uiModule)
        }

        if (BuildConfig.UPDRAFT_APP_KEY.isNotEmpty()) {
            val settings = Settings().apply {
                setAppKey(BuildConfig.UPDRAFT_APP_KEY)
                if (BuildConfig.UPDRAFT_SDK_KEY.isNotEmpty()) {
                    setSdkKey(BuildConfig.UPDRAFT_SDK_KEY)
                }
                setStoreRelease(false)
                setLogLevel(Settings.LOG_LEVEL_DEBUG)
            }
            Updraft.initialize(this, settings)
            Updraft.getInstance().start()
        }
    }
}
