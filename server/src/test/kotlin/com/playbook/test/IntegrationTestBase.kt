package com.playbook.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.playbook.module
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
abstract class IntegrationTestBase {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("playbook_test")
            withUsername("test")
            withPassword("test")
        }

        const val TEST_JWT_SECRET = "test-secret-do-not-use-in-production-at-all"

        @BeforeAll
        @JvmStatic
        fun resetSchema() {
            require(postgres.jdbcUrl.contains("localhost")) {
                "Refusing to clean non-local DB: ${postgres.jdbcUrl}"
            }
            // Flyway first — schema must exist before Exposed connects
            Flyway.configure()
                .dataSource(postgres.jdbcUrl, "test", "test")
                .locations("classpath:db/migrations")
                .cleanDisabled(false)
                .load()
                .apply { clean(); migrate() }
            // Connect Exposed so @BeforeAll seed() calls can use transaction { }
            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = "test",
                password = "test"
            )
        }
    }

    protected fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.url" to postgres.jdbcUrl,
                "database.user" to "test",
                "database.password" to "test",
                "database.maxPoolSize" to "5",
                "smtp.host" to "localhost",
                "smtp.port" to "1025",
                "smtp.from" to "test@playbook.test",
                "jwt.secret" to TEST_JWT_SECRET,
                "jwt.issuer" to "playbook",
                "jwt.audience" to "playbook-app",
                "jwt.expirationHours" to "1",
                "app.baseUrl" to "http://localhost:8080",
                "cors.allowedHost" to "localhost:3000",
                "billing.rateChf" to "1.0",
                "sa.password" to "test-sa-password"
            )
        }
        application { module() }
        block()
    }

    protected fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    protected fun bearerToken(userId: String, audience: String = "playbook-app"): String =
        JWT.create()
            .withIssuer("playbook")
            .withAudience(audience)
            .withClaim("sub", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

    protected fun saToken(userId: String): String = bearerToken(userId, "playbook-sa")

    protected fun expiredToken(userId: String): String =
        JWT.create()
            .withIssuer("playbook")
            .withAudience("playbook-app")
            .withClaim("sub", userId)
            .withExpiresAt(Date(System.currentTimeMillis() - 1000))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))
}
