package com.playbook.storage

interface FileStorageService {
    /**
     * Save file bytes, return the relative path (e.g. "avatar/uuid.jpg").
     * Caller can derive the public URL as "/uploads/{path}".
     */
    suspend fun save(bytes: ByteArray, type: FileType, extension: String): String

    /** Delete a previously saved file by its relative path. No-op if not found. */
    suspend fun delete(path: String)
}

enum class FileType(val dir: String) {
    AVATAR("avatar"),
    CLUB_LOGO("logo")
}
