package com.playbook.routes

import com.playbook.db.repositories.NotificationRepositoryImpl
import com.playbook.domain.RegisterPushTokenRequest
import com.playbook.domain.UpdateNotificationSettingsRequest
import com.playbook.plugins.userId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.registerNotificationRoutes() {
    val notificationRepo: NotificationRepositoryImpl by inject()

    // NT-028: GET /notifications?page=0&limit=20
    get("/notifications") {
        val uid = call.userId
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val result = notificationRepo.getNotifications(uid, page, limit)
        call.respond(result)
    }

    // NT-029: PUT /notifications/{id}/read
    put("/notifications/{id}/read") {
        val notificationId = call.parameters["id"]!!
        val uid = call.userId
        notificationRepo.markRead(notificationId, uid)
        call.respond(HttpStatusCode.NoContent)
    }

    // NT-030: PUT /notifications/read-all
    put("/notifications/read-all") {
        val uid = call.userId
        notificationRepo.markAllRead(uid)
        call.respond(HttpStatusCode.NoContent)
    }

    // NT-031: DELETE /notifications/{id}
    delete("/notifications/{id}") {
        val notificationId = call.parameters["id"]!!
        val uid = call.userId
        notificationRepo.deleteNotification(notificationId, uid)
        call.respond(HttpStatusCode.NoContent)
    }

    // NT-032: GET /notifications/settings
    get("/notifications/settings") {
        val uid = call.userId
        val settings = notificationRepo.getSettings(uid)
        call.respond(settings)
    }

    // NT-033: PUT /notifications/settings
    put("/notifications/settings") {
        val uid = call.userId
        val request = call.receive<UpdateNotificationSettingsRequest>()
        val settings = notificationRepo.upsertSettings(uid, request)
        call.respond(settings)
    }

    // NT-034: POST /push-tokens
    post("/push-tokens") {
        val uid = call.userId
        val request = call.receive<RegisterPushTokenRequest>()
        notificationRepo.registerPushToken(uid, request.platform, request.token)
        call.respond(HttpStatusCode.NoContent)
    }

    // NT-035: DELETE /push-tokens/{token}
    delete("/push-tokens/{token}") {
        val uid = call.userId
        val token = call.parameters["token"]!!
        notificationRepo.deregisterPushToken(uid, token)
        call.respond(HttpStatusCode.NoContent)
    }
}
