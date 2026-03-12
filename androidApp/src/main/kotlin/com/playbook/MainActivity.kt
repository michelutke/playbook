package com.playbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This will be wired to composeApp entry point in next plans
        setContent {
            // Placeholder
        }
    }
}
