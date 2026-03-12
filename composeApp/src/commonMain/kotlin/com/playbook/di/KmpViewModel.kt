package com.playbook.di

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope

expect abstract class KmpViewModel() {
    val viewModelScope: CoroutineScope
}
