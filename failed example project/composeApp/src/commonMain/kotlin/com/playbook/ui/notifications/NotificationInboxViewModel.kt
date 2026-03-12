package com.playbook.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.DeepLinkDestination
import com.playbook.domain.DeepLinkRouter
import com.playbook.domain.Notification
import com.playbook.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationInboxState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class NotificationInboxAction {
    object Load : NotificationInboxAction()
    data class MarkRead(val id: String) : NotificationInboxAction()
    object MarkAllRead : NotificationInboxAction()
    data class Delete(val id: String) : NotificationInboxAction()
    data class TapNotification(val notification: Notification) : NotificationInboxAction()
}

class NotificationInboxViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationInboxState())
    val state: StateFlow<NotificationInboxState> = _state.asStateFlow()

    var onNavigate: ((DeepLinkDestination) -> Unit)? = null

    init {
        submitAction(NotificationInboxAction.Load)
        observeUnreadCount()
    }

    fun submitAction(action: NotificationInboxAction) {
        when (action) {
            is NotificationInboxAction.Load -> load()
            is NotificationInboxAction.MarkRead -> markRead(action.id)
            is NotificationInboxAction.MarkAllRead -> markAllRead()
            is NotificationInboxAction.Delete -> delete(action.id)
            is NotificationInboxAction.TapNotification -> tapNotification(action.notification)
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                repository.syncFromServer()
                val paged = repository.getNotifications()
                _state.update { it.copy(notifications = paged.items, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load notifications") }
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            repository.getUnreadCount().collect { count ->
                _state.update { it.copy(unreadCount = count) }
            }
        }
    }

    private fun markRead(id: String) {
        viewModelScope.launch {
            try {
                repository.markRead(id)
            } catch (_: Exception) {
            }
            _state.update { s ->
                s.copy(notifications = s.notifications.map { if (it.id == id) it.copy(read = true) else it })
            }
        }
    }

    private fun markAllRead() {
        viewModelScope.launch {
            try {
                repository.markAllRead()
            } catch (_: Exception) {
            }
            _state.update { s -> s.copy(notifications = s.notifications.map { it.copy(read = true) }) }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteNotification(id)
            } catch (_: Exception) {
            }
            _state.update { s -> s.copy(notifications = s.notifications.filter { it.id != id }) }
        }
    }

    private fun tapNotification(notification: Notification) {
        if (!notification.read) markRead(notification.id)
        val destination = DeepLinkRouter.resolve(notification.deepLink)
        onNavigate?.invoke(destination)
    }
}
