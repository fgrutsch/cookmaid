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
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

allprojects {
    version = rootProject.scmVersion.version
}

val dockerPrereqs = listOf(":server:installDist", ":composeApp:wasmJsBrowserDistribution")
val dockerCacheArgs = if (System.getenv("GITHUB_ACTIONS") == "true") {
    listOf("--cache-from", "type=gha", "--cache-to", "type=gha,mode=max")
} else {
    emptyList()
}

tasks.register<Exec>("buildDockerImage") {
    group = "docker"
    description = "Build the cookmaid Docker image for the local architecture and load it into the daemon."
    dependsOn(dockerPrereqs)
    commandLine(
        listOf(
            "docker", "buildx", "build",
            "--load",
            "-f", "docker/Dockerfile",
            "-t", "cookmaid:${rootProject.version}",
            "-t", "cookmaid:latest",
        ) + dockerCacheArgs + ".",
    )
}

tasks.register<Exec>("pushDockerImage") {
    group = "docker"
    description = "Build and push the cookmaid Docker image. Requires -Pdocker.registry=<registry>."
    dependsOn(dockerPrereqs)
    val registry = findProperty("docker.registry")?.toString() ?: ""
    val version = rootProject.version.toString()
    commandLine(
        listOf(
            "docker", "buildx", "build",
            "--platform", "linux/amd64,linux/arm64",
            "--push",
            "-f", "docker/Dockerfile",
            "-t", "$registry:$version",
            "-t", "$registry:latest",
        ) + dockerCacheArgs + ".",
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
