plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Updraft SDK 1.0.7 pulls in com.simplify:ink and com.rm:freedrawview as transitive
// deps for its screenshot-annotation drawing UI. Neither exists on Maven Central,
// Google, or JitPack. Exclude globally — core update functionality still works.
configurations.configureEach {
    exclude(group = "com.simplify", module = "ink")
    exclude(group = "com.rm", module = "freedrawview")
}

kotlin {
    androidTarget()
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
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
            implementation(libs.updraft.sdk)
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
        
        buildConfigField("String", "UPDRAFT_APP_KEY", "\"${System.getenv("UPDRAFT_APP_KEY") ?: ""}\"")
        buildConfigField("String", "UPDRAFT_SDK_KEY", "\"${System.getenv("UPDRAFT_SDK_KEY") ?: ""}\"")
    }
    buildFeatures {
        buildConfig = true
    }
}