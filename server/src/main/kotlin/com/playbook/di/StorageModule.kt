package com.playbook.di

import com.playbook.storage.FileStorageService
import com.playbook.storage.LocalFileStorageService
import org.koin.dsl.module
import java.io.File

val StorageModule = module {
    single<FileStorageService> {
        LocalFileStorageService(File(System.getenv("UPLOADS_DIR") ?: "uploads"))
    }
}
