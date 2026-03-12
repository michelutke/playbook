package com.playbook.push

import com.onesignal.OneSignal
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import com.playbook.repository.PushTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object PushTokenManager {
    fun observe(scope: CoroutineScope, repository: PushTokenRepository) {
        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                val id = state.current.id ?: return
                scope.launch {
                    runCatching { repository.registerToken("android", id) }
                }
            }
        })
    }
}
