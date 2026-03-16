plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "io.github.fgrutsch.cookmaid"
version = "1.0.0"

kotlin {
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
application {
    mainClass.set("io.github.fgrutsch.cookmaid.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.shared)
    runtimeOnly(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.flyway)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.bundles.koin.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.nimbus.jose.jwt)
}
