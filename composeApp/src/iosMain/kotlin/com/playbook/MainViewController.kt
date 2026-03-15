package com.playbook

import androidx.compose.ui.window.ComposeUIViewController
import com.playbook.di.sharedModule
import com.playbook.di.uiModule
import org.koin.core.context.startKoin

/**
 * Called once from iOSApp.swift init() to start Koin DI.
 */
fun initKoin() {
    startKoin {
        modules(sharedModule, uiModule)
    }
}

/**
 * Entry point for SwiftUI via UIViewControllerRepresentable.
 * Renders the full Compose Multiplatform app on iOS.
 */
fun MainViewController() = ComposeUIViewController {
    PlaybookApp()
}
