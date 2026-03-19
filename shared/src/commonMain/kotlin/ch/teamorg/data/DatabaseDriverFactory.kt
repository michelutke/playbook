package ch.teamorg.data

import app.cash.sqldelight.db.SqlDriver
import ch.teamorg.db.TeamorgDb

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): TeamorgDb {
    return TeamorgDb(driverFactory.createDriver())
}
