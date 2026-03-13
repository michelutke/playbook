package com.playbook.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

actual abstract class KmpViewModel : ViewModel() {
    actual val viewModelScope: CoroutineScope = (this as ViewModel).viewModelScope
}
