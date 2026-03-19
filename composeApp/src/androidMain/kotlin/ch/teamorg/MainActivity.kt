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
        setContent {
            TeamorgApp(deepLinkToken = intent.inviteToken())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.inviteToken()?.let { token ->
            DeepLinkHandler.pendingToken.value = token
        }
    }
}

private fun Intent?.inviteToken(): String? =
    this?.data?.takeIf { it.scheme == "teamorg" }?.pathSegments?.lastOrNull()
