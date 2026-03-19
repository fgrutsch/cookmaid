plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

tasks.register("detektAll") {
    description = "Runs detekt with type resolution on all modules"
    group = "verification"
    dependsOn(
        ":server:detektMain",
        ":server:detektTest",
        ":shared:detektMainJvm",
        ":shared:detektTestJvm",
        ":composeApp:detektDevDebugAndroid",
    )
}