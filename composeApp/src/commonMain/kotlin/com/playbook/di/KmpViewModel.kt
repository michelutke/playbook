package com.playbook.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

/**
 * Android: delegates to koinViewModel() — proper ViewModelStore scoping.
 * iOS: uses remember{} + koin.get(clazz) directly, bypassing KoinViewModelFactory/
 *      AndroidParametersHolder which crashes on iOS via IrLinkageError.
 */
@Composable
inline fun <reified T : ViewModel> kmpViewModel(
    key: String? = null,
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T = kmpViewModelImpl(T::class, key, qualifier, parameters)

@Composable
expect fun <T : ViewModel> kmpViewModelImpl(
    clazz: KClass<T>,
    key: String?,
    qualifier: Qualifier?,
    parameters: ParametersDefinition?,
): T
