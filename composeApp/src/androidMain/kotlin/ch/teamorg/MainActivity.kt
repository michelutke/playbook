package ch.teamorg

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Set token before setContent so it's available on the first composition frame
        intent.inviteToken()?.let { DeepLinkHandler.pendingToken.value = it }
        setContent { TeamorgApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.inviteToken()?.let { DeepLinkHandler.pendingToken.value = it }
    }
}

private fun Intent?.inviteToken(): String? =
    this?.data?.takeIf { it.scheme == "teamorg" }?.pathSegments?.lastOrNull()
