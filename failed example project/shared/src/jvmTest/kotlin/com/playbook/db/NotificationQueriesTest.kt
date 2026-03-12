package com.playbook.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NotificationQueriesTest {

    private lateinit var driver: app.cash.sqldelight.db.SqlDriver
    private lateinit var db: PlaybookDatabase

    @BeforeEach
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PlaybookDatabase.Schema.create(driver)
        db = PlaybookDatabase(driver)
    }

    @AfterEach
    fun teardown() {
        driver.close()
    }

    private fun upsert(
        id: String,
        userId: String = "u1",
        type: String = "event_created",
        title: String = "Title",
        body: String = "Body",
        deepLink: String = "playbook://events/e1",
        referenceId: String? = null,
        read: Long = 0,
        createdAt: String = "2025-01-01T00:00:00Z",
    ) {
        db.notificationsQueries.upsert(
            id = id,
            userId = userId,
            type = type,
            title = title,
            body = body,
            deepLink = deepLink,
            referenceId = referenceId,
            read = read,
            createdAt = createdAt,
        )
    }

    @Test
    fun `upsert inserts notification and getAll returns it`() {
        upsert("n1")
        val all = db.notificationsQueries.getAll().executeAsList()
        assertEquals(1, all.size)
        assertEquals("n1", all.first().id)
        assertEquals("Title", all.first().title)
    }

    @Test
    fun `upsert replaces existing notification with same id`() {
        upsert("n1", title = "Old Title")
        upsert("n1", title = "New Title")
        val all = db.notificationsQueries.getAll().executeAsList()
        assertEquals(1, all.size)
        assertEquals("New Title", all.first().title)
    }

    @Test
    fun `getAll returns empty when no notifications`() {
        val all = db.notificationsQueries.getAll().executeAsList()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `getUnreadCount returns count of unread notifications`() {
        upsert("n1", read = 0)
        upsert("n2", read = 0)
        upsert("n3", read = 1)
        val count = db.notificationsQueries.getUnreadCount().executeAsOne()
        assertEquals(2L, count)
    }

    @Test
    fun `getUnreadCount returns 0 when all notifications are read`() {
        upsert("n1", read = 1)
        upsert("n2", read = 1)
        val count = db.notificationsQueries.getUnreadCount().executeAsOne()
        assertEquals(0L, count)
    }

    @Test
    fun `getUnreadCount returns 0 when table is empty`() {
        val count = db.notificationsQueries.getUnreadCount().executeAsOne()
        assertEquals(0L, count)
    }

    @Test
    fun `markRead sets read to 1 for specific notification`() {
        upsert("n1", read = 0)
        upsert("n2", read = 0)
        db.notificationsQueries.markRead("n1")
        val count = db.notificationsQueries.getUnreadCount().executeAsOne()
        assertEquals(1L, count)
        val n1 = db.notificationsQueries.getById("n1").executeAsOne()
        assertEquals(1L, n1.read)
    }

    @Test
    fun `markAllRead sets all notifications to read and getUnreadCount returns 0`() {
        upsert("n1", read = 0)
        upsert("n2", read = 0)
        upsert("n3", read = 0)
        db.notificationsQueries.markAllRead()
        val count = db.notificationsQueries.getUnreadCount().executeAsOne()
        assertEquals(0L, count)
    }

    @Test
    fun `delete removes specific notification`() {
        upsert("n1")
        upsert("n2")
        db.notificationsQueries.delete("n1")
        val all = db.notificationsQueries.getAll().executeAsList()
        assertEquals(1, all.size)
        assertEquals("n2", all.first().id)
    }

    @Test
    fun `deleteAll removes all notifications and getAll returns empty`() {
        upsert("n1")
        upsert("n2")
        upsert("n3")
        db.notificationsQueries.deleteAll()
        val all = db.notificationsQueries.getAll().executeAsList()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `getById returns notification by id`() {
        upsert("n1", type = "event_cancelled", title = "Cancelled")
        val n = db.notificationsQueries.getById("n1").executeAsOneOrNull()
        assertNotNull(n)
        assertEquals("event_cancelled", n.type)
        assertEquals("Cancelled", n.title)
    }

    @Test
    fun `getAll returns notifications ordered by createdAt descending`() {
        upsert("n1", createdAt = "2025-01-01T00:00:00Z")
        upsert("n2", createdAt = "2025-01-03T00:00:00Z")
        upsert("n3", createdAt = "2025-01-02T00:00:00Z")
        val all = db.notificationsQueries.getAll().executeAsList()
        assertEquals("n2", all[0].id)
        assertEquals("n3", all[1].id)
        assertEquals("n1", all[2].id)
    }
}
