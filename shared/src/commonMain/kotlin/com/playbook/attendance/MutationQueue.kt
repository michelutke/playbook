package com.playbook.attendance

import com.playbook.db.AttendanceQueries
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

data class PendingMutation(
    val id: String,
    val type: String,
    val payload: String,
    val createdAt: String,
)

class MutationQueue(
    private val queries: AttendanceQueries,
) {
    private val mutex = Mutex()

    suspend fun enqueue(type: String, payload: String): String {
        val id = Clock.System.now().toEpochMilliseconds().toString()
        val createdAt = Clock.System.now().toString()
        mutex.withLock {
            queries.insertMutation(id = id, type = type, payload = payload, createdAt = createdAt)
        }
        return id
    }

    suspend fun dequeue(id: String) {
        mutex.withLock {
            queries.deleteMutation(id = id)
        }
    }

    suspend fun getAll(): List<PendingMutation> = mutex.withLock {
        queries.getPendingMutations().executeAsList().map {
            PendingMutation(
                id = it.id,
                type = it.type,
                payload = it.payload,
                createdAt = it.created_at,
            )
        }
    }

    suspend fun isEmpty(): Boolean = mutex.withLock {
        queries.getPendingMutations().executeAsList().isEmpty()
    }
}
