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
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.coil3.compose)
        }
        
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.lifecycle.process)
            implementation(libs.activity.compose)
        }

        iosMain.dependencies {
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

// kotlinx-datetime 0.7.x typealiases (Instant = kotlin.time.Instant) cause a Kotlin/Native IR
// ClassCastException during framework linking with CMP 1.10.1. Force 0.6.0 until upstream fix lands.
configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
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