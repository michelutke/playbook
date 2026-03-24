package ch.teamorg

import androidx.compose.runtime.mutableStateOf

object DeepLinkHandler {
    val pendingToken = mutableStateOf<String?>(null)
}
