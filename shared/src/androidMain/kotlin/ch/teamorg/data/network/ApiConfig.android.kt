package ch.teamorg.data.network

import ch.teamorg.shared.BuildConfig

actual object ApiConfig {
    actual val baseUrl: String = BuildConfig.API_BASE_URL
}
