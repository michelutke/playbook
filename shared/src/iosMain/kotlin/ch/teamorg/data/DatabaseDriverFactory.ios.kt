package ch.teamorg.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import ch.teamorg.db.TeamorgDb

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(TeamorgDb.Schema, "teamorg.db")
    }
}
