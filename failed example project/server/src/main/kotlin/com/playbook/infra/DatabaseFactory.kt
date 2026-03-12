package com.playbook.infra

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val datasource = createHikariDataSource(config)
        runFlyway(datasource)
        Database.connect(datasource)
    }

    private fun createHikariDataSource(config: ApplicationConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.property("database.url").getString()
            username = config.property("database.user").getString()
            password = config.property("database.password").getString()
            maximumPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10
            minimumIdle = 1
            driverClassName = "org.postgresql.Driver"
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    private fun runFlyway(datasource: HikariDataSource) {
        Flyway.configure()
            .dataSource(datasource)
            .locations("classpath:db/migrations")
            .load()
            .migrate()
    }
}
