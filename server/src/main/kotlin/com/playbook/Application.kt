package com.playbook

import com.playbook.infra.startAutoPresentJob
import com.playbook.infra.startExportJobRunner
import com.playbook.infra.startMaterializationJob
import com.playbook.infra.startNotificationScheduler
import com.playbook.plugins.configureAuth
import com.playbook.plugins.configureKoin
import com.playbook.plugins.configureRouting
import com.playbook.plugins.configureSerialization
import com.playbook.push.NotificationService
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.ktor.ext.inject

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureAuth()
    configureKoin()
    configureRouting()
    startMaterializationJob()
    startExportJobRunner()
    startAutoPresentJob()
    val notificationService: NotificationService by inject()
    startNotificationScheduler(notificationService)
}
