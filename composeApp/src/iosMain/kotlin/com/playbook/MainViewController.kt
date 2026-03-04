package com.playbook

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.playbook.data.network.ApiConfig
import com.playbook.di.iosComposeModule
import com.playbook.di.iosPlatformModule
import com.playbook.di.sharedModule
import com.playbook.di.uiModule
import org.koin.core.context.startKoin
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

private var koinInitialized = false
private val iosViewModelStore = ViewModelStore()

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        koinInitialized = true
        startKoin {
            modules(
                iosPlatformModule(),
                sharedModule(
                    ApiConfig(
                        baseUrl = "http://localhost:8080",
                        authTokenProvider = {
                            NSUserDefaults.standardUserDefaults.stringForKey("auth_token")
                        }
                    )
                ),
                uiModule,
                iosComposeModule,
            )
        }
    }
    return ComposeUIViewController {
        val owner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore get() = iosViewModelStore
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            PlaybookApp()
        }
    }
}
