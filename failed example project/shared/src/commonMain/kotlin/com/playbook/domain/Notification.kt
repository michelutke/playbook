package com.playbook.domain

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val deepLink: String,
    val referenceId: String? = null,
    val read: Boolean,
    val createdAt: String // ISO-8601
)

@Serializable
data class NotificationSettings(
    val userId: String,
    val newEvents: Boolean = true,
    val eventChanges: Boolean = true,
    val eventCancellations: Boolean = true,
    val reminders: Boolean = true,
    val reminderLeadTime: String = "1d",
    val attendancePerResponse: Boolean = true,
    val attendanceSummary: Boolean = false,
    val attendanceSummaryLeadTime: String = "2h",
    val abwesenheitChanges: Boolean = true
)

@Serializable
data class PushToken(
    val platform: String,
    val token: String
)

@Serializable
data class RegisterPushTokenRequest(
    val platform: String,
    val token: String,
)

@Serializable
data class PagedNotifications(
    val items: List<Notification>,
    val page: Int,
    val total: Int
)

@Serializable
data class UpdateNotificationSettingsRequest(
    val newEvents: Boolean? = null,
    val eventChanges: Boolean? = null,
    val eventCancellations: Boolean? = null,
    val reminders: Boolean? = null,
    val reminderLeadTime: String? = null,
    val attendancePerResponse: Boolean? = null,
    val attendanceSummary: Boolean? = null,
    val attendanceSummaryLeadTime: String? = null,
    val abwesenheitChanges: Boolean? = null
)
