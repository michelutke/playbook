plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content-negotiation)
            implementation(libs.ktor.serialization-kotlinx-json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization-json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.multiplatform-settings)
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android-driver)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native-driver)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.sqldelight.sqlite-driver)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.playbook.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "API_BASE_URL", "\"${System.getenv("API_BASE_URL") ?: project.findProperty("API_BASE_URL") ?: "https://api.playbook.app"}\"")
    }
    buildFeatures {
        buildConfig = true
    }
}

sqldelight {
    databases {
        create("PlaybookDb") {
            packageName.set("com.playbook.db")
        }
    }
}