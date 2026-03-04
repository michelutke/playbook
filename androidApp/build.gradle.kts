import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

android {
    namespace = "com.playbook.android"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.playbook.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "ONESIGNAL_APP_ID", "\"${project.findProperty("ONESIGNAL_APP_ID") ?: ""}\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"${project.findProperty("BACKEND_BASE_URL") ?: "http://10.0.2.2:8080"}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":composeApp"))
    // Compose — explicit coords; version managed by compose.multiplatform plugin
    implementation("org.jetbrains.compose.ui:ui")
    implementation("org.jetbrains.compose.foundation:foundation")
    implementation("org.jetbrains.compose.material3:material3")
    implementation("org.jetbrains.compose.runtime:runtime")
    implementation("org.jetbrains.compose.material:material-icons-extended")
    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androidx.compose)
    // Navigation
    implementation(libs.navigation.compose)
    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)
    // Activity
    implementation(libs.activity.compose)
    // Image loading (Coil 2 for legacy androidApp screens)
    implementation(libs.coil.compose)
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    // DateTime
    implementation(libs.kotlinx.datetime)
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // OneSignal
    implementation("com.onesignal:OneSignal:[5.6.1, 5.99.99]")
}
