package com.playbook.push

import android.content.Context
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

object OneSignalInitializer {
    fun init(context: Context, appId: String) {
        OneSignal.Debug.logLevel = LogLevel.NONE
        OneSignal.initWithContext(context, appId)
    }

    fun login(userId: String) {
        OneSignal.login(userId)
    }

    fun logout() {
        OneSignal.logout()
    }

    fun getSubscriptionId(): String? = OneSignal.User.pushSubscription.id
}
