package com.playbook

import android.app.Application
import com.playbook.di.sharedModule
import com.playbook.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlaybookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@PlaybookApplication)
            modules(sharedModule, uiModule)
        }
    }
}
