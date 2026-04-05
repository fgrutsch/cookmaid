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

tasks.register<Exec>("buildDockerImage") {
    group = "docker"
    description = "Build the cookmaid Docker image for all architectures."
    dependsOn(":server:installDist", ":composeApp:wasmJsBrowserProductionWebpack")
    commandLine(
        "docker", "buildx", "build",
        "--platform", "linux/amd64,linux/arm64",
        "-f", "docker/Dockerfile",
        "-t", "cookmaid:${rootProject.version}",
        "-t", "cookmaid:latest",
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
    )
}