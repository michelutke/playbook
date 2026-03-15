plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}


kotlin {
    androidTarget()
    
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.navigation3)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil3.compose)
            implementation(libs.lc.viewmodel.compose)
        }
        
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.lifecycle.process)
            implementation(libs.activity.compose)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.compose.ui.test.manifest)
            }
        }
    }
    
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

// navigationevent-compose-iossimulatorarm64 only exists up to 1.0.0-alpha07 on Google Maven.
// Gradle conflict resolution can upgrade it to 1.0.2 (stable, Android-only) which has no
// iOS variant. Pin it to 1.0.0-alpha07 for iOS/native configurations only.
configurations.configureEach {
    if (name.contains("ios", ignoreCase = true) || name.contains("native", ignoreCase = true)) {
        resolutionStrategy.force("androidx.navigationevent:navigationevent-compose:1.0.0-alpha07")
    }
}

android {
    namespace = "com.playbook.compose"
    compileSdk = 36
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
    }
    buildFeatures {
        buildConfig = true
    }
}