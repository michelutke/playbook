package com.playbook

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.playbook.di.sharedModule
import com.playbook.di.uiModule
import org.koin.core.context.startKoin

private var koinInitialized = false

fun initKoin() {
    if (!koinInitialized) {
        startKoin {
            modules(sharedModule, uiModule)
        }
        koinInitialized = true
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    PlaybookApp()
}
