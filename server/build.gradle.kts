plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    application
}

group = "io.github.fgrutsch.cookmaid"
version = rootProject.version

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

tasks.register<Exec>("buildDockerImage") {
    group = "docker"
    description = "Build the cookmaid Docker image."
    dependsOn(tasks.named("installDist"), ":composeApp:wasmJsBrowserProductionWebpack")
    workingDir(rootProject.projectDir)
    commandLine("docker", "build", "-t", "cookmaid:${project.version}", "-t", "cookmaid:latest", ".")
}

tasks.register("printVersion") {
    group = "help"
    description = "Print the project version."
    val version = project.version.toString()
    doLast { println(version) }
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
