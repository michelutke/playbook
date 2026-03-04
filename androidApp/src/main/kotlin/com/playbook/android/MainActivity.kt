package com.playbook.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.playbook.PlaybookApp

class MainActivity : ComponentActivity() {
    private var deepLinkToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkToken = extractDeepLinkToken(intent)
        setContent {
            PlaybookApp(deepLinkToken = deepLinkToken)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkToken = extractDeepLinkToken(intent)
    }

    private fun extractDeepLinkToken(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "playbook") return null
        return uri.getQueryParameter("token")
    }
}
