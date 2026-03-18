package ch.teamorg

import android.app.Application
import ch.teamorg.di.sharedModule
import ch.teamorg.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TeamorgApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TeamorgApplication)
            modules(sharedModule, uiModule)
        }
    }
}
