package com.playbook

import androidx.compose.ui.window.ComposeUIViewController
import com.playbook.data.network.ApiConfig
import com.playbook.di.iosComposeModule
import com.playbook.di.iosPlatformModule
import com.playbook.di.sharedModule
import com.playbook.di.uiModule
import org.koin.core.context.startKoin
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

private var koinInitialized = false

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
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
        koinInitialized = true
    }
    return ComposeUIViewController {
        PlaybookApp()
    }
}
