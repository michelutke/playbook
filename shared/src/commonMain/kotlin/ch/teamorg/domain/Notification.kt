package ch.teamorg.domain

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    val type: String,           // event_new, event_edit, event_cancel, reminder, response, absence
    val title: String,
    val body: String,
    val entityId: String? = null,
    val entityType: String? = null,  // event, abwesenheit
    val isRead: Boolean = false,
    val createdAt: String          // ISO-8601 timestamp string
)

@Serializable
data class NotificationSettings(
    val userId: String,
    val teamId: String,
    val eventsNew: Boolean = true,
    val eventsEdit: Boolean = true,
    val eventsCancel: Boolean = true,
    val remindersEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 120,
    val coachResponseMode: String = "per_response",  // "per_response" or "summary"
    val absencesEnabled: Boolean = true
)

@Serializable
data class UpdateNotificationSettingsRequest(
    val eventsNew: Boolean? = null,
    val eventsEdit: Boolean? = null,
    val eventsCancel: Boolean? = null,
    val remindersEnabled: Boolean? = null,
    val reminderLeadMinutes: Int? = null,
    val coachResponseMode: String? = null,
    val absencesEnabled: Boolean? = null
)

@Serializable
data class ReminderOverride(
    val reminderLeadMinutes: Int?   // null = use global default
)

@Serializable
data class UnreadCountResponse(
    val count: Long
)

@Serializable
data class MarkAllReadResponse(
    val marked: Int
)
