package com.playbook.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

@Composable
actual fun <T : ViewModel> kmpViewModelImpl(
    clazz: KClass<T>,
    key: String?,
    qualifier: Qualifier?,
    parameters: ParametersDefinition?,
): T = koinViewModel(viewModelClass = clazz, key = key, qualifier = qualifier, parameters = parameters)
