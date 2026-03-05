import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val xcf = XCFramework()
    listOf(
        iosSimulatorArm64(),
        iosArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "composeApp"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.materialIconsExtended)
            // Navigation 3
            implementation(libs.navigation3.ui)
            // Coil 3
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor3)
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // ViewModel
            implementation(libs.lifecycle.viewmodel)
            // Settings
            implementation(libs.multiplatform.settings)
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            // DateTime (forced to 0.6.0 — 0.7.x typealiases cause Kotlin/Native IR crash with CMP 1.10.1)
            implementation(libs.kotlinx.datetime)
            // Serialization
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.activity.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation("androidx.datastore:datastore-preferences:1.1.1")
            implementation("com.onesignal:OneSignal:5.6.1")
        }
    }
}

// kotlinx-datetime 0.7.x typealiases (Instant = kotlin.time.Instant) cause a Kotlin/Native IR
// ClassCastException during framework linking with CMP 1.10.1. Force 0.6.0 until the upstream
// KT-XXXXX / kotlinx-datetime#NNN fix lands.
configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
}

android {
    namespace = "com.playbook.composeapp"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
