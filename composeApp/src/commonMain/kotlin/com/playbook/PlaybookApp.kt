package com.playbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.playbook.auth.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaybookApp() {
    // AuthViewModel injected — Phase 1 wires navigation based on authState
    @Suppress("UNUSED_VARIABLE")
    val authViewModel = koinViewModel<AuthViewModel>()
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize())
    }
}
