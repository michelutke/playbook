package com.playbook.di

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope

expect abstract class KmpViewModel() : ViewModel {
    val viewModelScope: CoroutineScope
}