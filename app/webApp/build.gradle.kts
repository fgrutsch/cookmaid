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
            implementation(compose.runtime)
            implementation(compose.ui)
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

    // Absent in CI/production — early-exit to leave ${VAR} placeholders intact for envsubst.
    val localPropsFile = rootProject.file("dev/local.properties").takeIf { it.exists() } ?: return@named
    val localProps = Properties().apply { localPropsFile.reader().use { load(it) } }

    filesMatching("index.html") {
        expand(
            "OIDC_DISCOVERY_URI" to localProps.getProperty("oidc.discoveryUri"),
            "OIDC_CLIENT_ID" to localProps.getProperty("oidc.clientId"),
            "OIDC_SCOPE" to localProps.getProperty("oidc.scope"),
            "OIDC_ACCOUNT_URI" to localProps.getProperty("oidc.accountUri"),
            "OIDC_RESOURCE" to (localProps.getProperty("oidc.resource") ?: ""),
        )
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
