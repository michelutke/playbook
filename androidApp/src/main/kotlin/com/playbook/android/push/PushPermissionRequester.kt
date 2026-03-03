package com.playbook.android.push

import com.onesignal.OneSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun requestPushPermission(): Flow<Boolean> = flow {
    OneSignal.Notifications.requestPermission(true)
    emit(OneSignal.Notifications.permission)
}
