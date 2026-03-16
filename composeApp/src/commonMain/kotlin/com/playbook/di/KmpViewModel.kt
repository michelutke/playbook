package com.playbook.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatformTools

expect abstract class KmpViewModel() : ViewModel {
    val viewModelScope: CoroutineScope
}

@Composable
inline fun <reified T : ViewModel> kmpViewModel(
    key: String? = null,
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T = remember(key) {
    KoinPlatformTools.defaultContext().get().get<T>(qualifier, parameters)
}
