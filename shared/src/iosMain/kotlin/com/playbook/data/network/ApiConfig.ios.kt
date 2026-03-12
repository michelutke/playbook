package com.playbook.data.network

import platform.Foundation.NSBundle

actual object ApiConfig {
    actual val baseUrl: String = NSBundle.mainBundle.objectForInfoDictionaryKey("API_BASE_URL") as? String ?: "http://localhost:8080"
}
