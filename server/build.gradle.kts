import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    application
}

group = "io.github.fgrutsch.cookmaid"

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

// Inject OIDC_CLIENT_ID from local.properties so `:server:run` works
// without exporting env vars. Same file the WasmJS build already reads.
tasks.named<JavaExec>("run") {
    val localPropsFile = rootProject.file("local.properties").takeIf { it.exists() } ?: return@named
    val localProps = Properties().apply { localPropsFile.reader().use { load(it) } }
    localProps.getProperty("oidc.clientId")?.let { environment("OIDC_CLIENT_ID", it) }
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        verify {
            rule {
                minBound(90)
            }
        }
    }
}

dependencies {
    implementation(projects.shared)
    runtimeOnly(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.kotlinx.datetime)
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
