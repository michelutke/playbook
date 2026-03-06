package com.playbook.infra

import io.ktor.server.config.*
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.mailer.MailerBuilder

object MailerFactory {
    fun create(config: ApplicationConfig): Mailer {
        val host = config.property("smtp.host").getString()
        val port = config.propertyOrNull("smtp.port")?.getString()?.toInt() ?: 587
        val user = config.propertyOrNull("smtp.user")?.getString()?.takeIf { it.isNotBlank() }
        val pass = config.propertyOrNull("smtp.password")?.getString()?.takeIf { it.isNotBlank() }

        val builder = MailerBuilder.withSMTPServer(host, port, user, pass)

        return builder.buildMailer()
    }
}
