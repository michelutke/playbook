package ch.teamorg.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val entityId: String?,
    val entityType: String?,
    val isRead: Boolean,
    val createdAt: String
)

@Serializable
data class NotificationSettingsResponse(
    val userId: String,
    val teamId: String,
    val eventsNew: Boolean,
    val eventsEdit: Boolean,
    val eventsCancel: Boolean,
    val remindersEnabled: Boolean,
    val reminderLeadMinutes: Int,
    val coachResponseMode: String,
    val absencesEnabled: Boolean
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
data class ReminderOverrideRequest(
    val reminderLeadMinutes: Int?
)
