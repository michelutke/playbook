package com.playbook.push

import com.onesignal.OneSignal

actual class PushPermissionRequester {
    actual suspend fun requestPermission(): Boolean {
        OneSignal.Notifications.requestPermission(true)
        return OneSignal.Notifications.permission
    }
}
