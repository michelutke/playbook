package com.playbook.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.currentKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import kotlin.reflect.KClass

@OptIn(KoinInternalApi::class)
@Composable
actual fun <T : ViewModel> kmpViewModelImpl(
    clazz: KClass<T>,
    key: String?,
    qualifier: Qualifier?,
    parameters: ParametersDefinition?,
): T {
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
        ?: error("No ViewModelStoreOwner provided via LocalViewModelStoreOwner")
    val extras = defaultExtras(viewModelStoreOwner)
    val scope = currentKoinScope()
    return resolveViewModel(clazz, viewModelStoreOwner.viewModelStore, key, extras, qualifier, scope, parameters)
}
