package com.playbook.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatformTools
import kotlin.reflect.KClass

/**
 * iOS: bypasses koinViewModel()/KoinViewModelFactory entirely.
 * Calls koin.get(clazz) which invokes the factory{} lambda directly —
 * no AndroidParametersHolder, no IrLinkageError.
 */
@Composable
actual fun <T : ViewModel> kmpViewModelImpl(
    clazz: KClass<T>,
    key: String?,
    qualifier: Qualifier?,
    parameters: ParametersDefinition?,
): T = remember(key) {
    KoinPlatformTools.defaultContext().get().get(clazz, qualifier, parameters)
}
