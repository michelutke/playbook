import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientLogging)
            implementation(libs.ktor.serializationKotlinxJson)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.sqldelight.runtime)
            implementation(libs.multiplatform.settings)
        }

        androidMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.onesignal)
        }

        iosMain.dependencies {
            implementation(libs.ktor.clientDarwin)
            implementation(libs.sqldelight.nativeDriver)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
            implementation(libs.sqldelight.sqliteDriver)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val jvmTest by getting {
            dependencies {
                implementation(project(":server"))
                implementation(libs.kotlin.testJunit)
                implementation(libs.ktor.serverNetty)
                implementation(libs.ktor.serverCore)
                implementation(libs.testcontainers.postgresql)
                implementation(libs.testcontainers.junit)
                implementation(libs.kotlinx.coroutinesTest)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "ch.teamorg.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        buildConfigField("String", "API_BASE_URL", "\"${System.getenv("API_BASE_URL") ?: project.findProperty("API_BASE_URL") ?: "https://api.teamorg.app"}\"")
    }
    buildTypes {
        debug {
            val localUrl = System.getenv("API_BASE_URL") ?: project.findProperty("API_BASE_URL")
            buildConfigField("String", "API_BASE_URL", "\"${localUrl ?: "http://10.0.2.2:8080"}\"")
        }
        release {
            val prodUrl = System.getenv("API_BASE_URL") ?: project.findProperty("API_BASE_URL")
            buildConfigField("String", "API_BASE_URL", "\"${prodUrl ?: "https://api.teamorg.app"}\"")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

sqldelight {
    databases {
        create("TeamorgDb") {
            packageName.set("ch.teamorg.db")
        }
    }
}
