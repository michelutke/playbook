package com.playbook

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
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
    // iOS has no ComponentActivity to auto-provide LocalNavigationEventDispatcherOwner.
    // nav3 dev2887 klib was compiled against Google navigationevent-compose (alpha07).
    // Must provide via the Google singleton so NavDisplay finds it (not JetBrains rc01).
    val dispatcher = remember { NavigationEventDispatcher() }
    val owner = remember(dispatcher) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = dispatcher
        }
    }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
        PlaybookApp()
    }
}
