package ch.teamorg.storage

import java.io.File
import java.util.UUID

class LocalFileStorageService(private val baseDir: File) : FileStorageService {
    override suspend fun save(bytes: ByteArray, type: FileType, extension: String): String {
        val dir = File(baseDir, type.dir).apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val filename = "${UUID.randomUUID()}.$extension"
        File(dir, filename).writeBytes(bytes)
        return "${type.dir}/$filename"
    }

    override suspend fun delete(path: String) {
        val file = File(baseDir, path)
        if (file.exists()) {
            file.delete()
        }
    }
}
