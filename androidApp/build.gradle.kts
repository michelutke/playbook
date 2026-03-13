plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.playbook"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.playbook"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "UPDRAFT_APP_KEY", "\"${System.getenv("UPDRAFT_APP_KEY") ?: ""}\"")
        buildConfigField("String", "UPDRAFT_SDK_KEY", "\"${System.getenv("UPDRAFT_SDK_KEY") ?: ""}\"")
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("${project.buildDir}/release.jks")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
        debug {
            // Use release signing for debug builds in CI if needed, 
            // but usually we just build release for Updraft
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.updraft.sdk)
}
