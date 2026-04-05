plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("pl.allegro.tech.build.axion-release") version "1.21.1"
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

version = scmVersion.version

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