package ch.teamorg.ui.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(onResult: (bytes: ByteArray, ext: String) -> Unit): () -> Unit
