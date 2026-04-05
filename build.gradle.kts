plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.axionRelease)
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

allprojects {
    version = rootProject.scmVersion.version
}

val dockerPrereqs = listOf(":server:installDist", ":composeApp:wasmJsBrowserProductionWebpack")
val dockerPlatforms = "linux/amd64,linux/arm64"

tasks.register<Exec>("buildDockerImage") {
    group = "docker"
    description = "Build the cookmaid Docker image for the local architecture (no push)."
    dependsOn(dockerPrereqs)
    commandLine(
        "docker", "build",
        "-f", "docker/Dockerfile",
        "-t", "cookmaid:${rootProject.version}",
        "-t", "cookmaid:latest",
        ".",
    )
}

tasks.register<Exec>("pushDockerImage") {
    group = "docker"
    description = "Build and push the cookmaid Docker image. Requires -Pdocker.registry=<registry>."
    dependsOn(dockerPrereqs)
    val registry = findProperty("docker.registry")?.toString() ?: ""
    val version = rootProject.version.toString()
    commandLine(
        "docker", "buildx", "build",
        "--platform", dockerPlatforms,
        "--push",
        "-f", "docker/Dockerfile",
        "-t", "$registry:$version",
        "-t", "$registry:latest",
        ".",
    )
}

tasks.register("detektAll") {
    description = "Runs detekt with type resolution on all modules"
    group = "verification"
    dependsOn(
        ":server:detektMain",
        ":server:detektTest",
        ":shared:detektMainJvm",
        ":shared:detektTestJvm",
        ":composeApp:detektMainAndroid",
        ":composeApp:detektCommonTestSourceSet",
        ":composeApp:detektWasmJsMainSourceSet",
    )
}