import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        optIn.add("org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.app.shared)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.bundles.oidc)
            implementation(libs.multiplatform.settings.no.arg)
        }
    }
}

tasks.named<Copy>("wasmJsProcessResources") {
    val appVersion = project.version.toString()

    filesMatching("service-worker.js") {
        filter { it.replace("__APP_VERSION__", appVersion) }
    }
}

tasks.named<Sync>("wasmJsBrowserDistribution") {
    doLast {
        val distDir = destinationDir
        val jsFile = distDir.listFiles()?.firstOrNull { it.name.matches(Regex("^app\\.[a-f0-9]+\\.js$")) }
            ?: error("No app.[hash].js found in $distDir")

        val indexHtml = File(distDir, "index.html")
        indexHtml.writeText(indexHtml.readText().replace("webApp.js", jsFile.name))
    }
}

// Overwrites the processed index.html with dev OIDC values from local.properties.
// Only the dev server depends on it, so production builds (wasmJsBrowserDistribution,
// the Docker image) never run it and keep the ${VAR} placeholders for the
// entrypoint's envsubst. Reads from source each run, so it stays idempotent.
val injectLocalOidcConfig by tasks.registering(Copy::class) {
    dependsOn("wasmJsProcessResources")

    val localPropsFile = rootProject.file("local.properties")
    require(localPropsFile.exists()) {
        "runLocal needs local.properties with oidc.* keys — see README → Configure local settings"
    }
    val localProps = Properties().apply { localPropsFile.reader().use { load(it) } }

    from("src/wasmJsMain/resources") {
        include("index.html")
        expand(
            "OIDC_DISCOVERY_URI" to localProps.getProperty("oidc.discoveryUri"),
            "OIDC_CLIENT_ID" to localProps.getProperty("oidc.clientId"),
            "OIDC_SCOPE" to localProps.getProperty("oidc.scope"),
            "OIDC_ACCOUNT_URI" to localProps.getProperty("oidc.accountUri"),
            "OIDC_RESOURCE" to (localProps.getProperty("oidc.resource") ?: ""),
        )
    }
    into(layout.buildDirectory.dir("processedResources/wasmJs/main"))
}

// The dev resource sync consumes processedResources, so it must run after the
// inject task overwrites index.html. Wiring the consumer (not just the run task)
// satisfies Gradle's task-output validation. Production uses its own
// (Production) sync, which never depends on inject → keeps the ${VAR} placeholders.
tasks.named("wasmJsDevelopmentExecutableCompileSync") {
    dependsOn(injectLocalOidcConfig)
}
