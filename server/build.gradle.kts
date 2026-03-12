plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.playbook"
version = "1.0.0"

application {
    mainClass.set("com.playbook.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content-negotiation)
    implementation(libs.ktor.serialization-kotlinx-json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth-jwt)
    implementation(libs.koin.ktor)
    implementation(libs.exposed-core)
    implementation(libs.exposed-jdbc)
    implementation(libs.hikaricp)
    implementation(libs.flyway-core)
    implementation(libs.flyway-database-postgresql)
    implementation(libs.postgresql)
    implementation(libs.simple-java-mail)
    implementation(libs.logback-classic)
    implementation("org.mindrot:jbcrypt:0.4")
    
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test)
    testImplementation("com.h2database:h2:2.3.232")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "21"
    }
}
