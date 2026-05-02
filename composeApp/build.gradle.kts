import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }

    android {
        namespace = "io.github.fgrutsch.cookmaid.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.materialkolor)
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.bundles.oidc)
            implementation(libs.bundles.ktor.client)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

buildkonfig {
    packageName = "io.github.fgrutsch.cookmaid"
    defaultConfigs {
        buildConfigField(STRING, "APP_VERSION", project.version.toString(), const = true)
    }
}

tasks.named<Copy>("wasmJsProcessResources") {
    val appVersion = project.version.toString()

    // Replace the version placeholder in the service worker cache name.
    filesMatching("service-worker.js") {
        filter { it.replace("__APP_VERSION__", appVersion) }
    }

    // local.properties is absent in CI/production — early-exit to leave ${VAR} placeholders
    // intact for docker-entrypoint.sh envsubst. Without this guard, getProperty() returns
    // null and Gradle writes the literal string "null" into index.html, breaking envsubst.
    val localPropsFile = rootProject.file("local.properties").takeIf { it.exists() } ?: return@named
    val localProps = Properties().apply { localPropsFile.reader().use { load(it) } }

    filesMatching("index.html") {
        expand(
            "OIDC_DISCOVERY_URI" to localProps.getProperty("oidc.discoveryUri"),
            "OIDC_CLIENT_ID" to localProps.getProperty("oidc.clientId"),
            "OIDC_SCOPE" to localProps.getProperty("oidc.scope"),
            "OIDC_ACCOUNT_URI" to localProps.getProperty("oidc.accountUri"),
        )
    }
}

tasks.named<Sync>("wasmJsBrowserDistribution") {
    doLast {
        val distDir = destinationDir
        val jsFile = distDir.listFiles()?.firstOrNull { it.name.matches(Regex("^app\\.[a-f0-9]+\\.js$")) }
            ?: error("No app.[hash].js found in $distDir")

        // Rewrite the JS bundle reference in index.html.
        val indexHtml = File(distDir, "index.html")
        indexHtml.writeText(indexHtml.readText().replace("composeApp.js", jsFile.name))
    }
}
