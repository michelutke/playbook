package com.playbook.data.network

import com.playbook.shared.BuildConfig

actual object ApiConfig {
    actual val baseUrl: String = BuildConfig.API_BASE_URL
}
