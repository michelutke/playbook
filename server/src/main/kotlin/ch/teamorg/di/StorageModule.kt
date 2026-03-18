package ch.teamorg.di

import ch.teamorg.storage.FileStorageService
import ch.teamorg.storage.LocalFileStorageService
import org.koin.dsl.module
import java.io.File

val StorageModule = module {
    single<FileStorageService> {
        LocalFileStorageService(File(System.getenv("UPLOADS_DIR") ?: "uploads"))
    }
}
