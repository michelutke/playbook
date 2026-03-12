package com.playbook.test

import com.playbook.module
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.util.*

abstract class IntegrationTestBase {
    fun withPlaybookTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        val testConfig = MapApplicationConfig(
            "jwt.secret" to "test_secret_32_chars_long_minimum_required",
            "jwt.issuer" to "playbook",
            "jwt.audience" to "playbook-users",
            "jwt.realm" to "playbook",
            "jwt.expiry-days" to "30",
            "database.url" to "jdbc:h2:mem:test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            "database.driver" to "org.h2.Driver"
        )
        
        environment {
            config = testConfig
        }

        application {
            module()
        }

        block()
    }
}
