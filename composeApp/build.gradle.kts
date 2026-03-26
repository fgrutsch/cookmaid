import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
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

tasks.named<Copy>("wasmJsProcessResources") {
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
    }
    filesMatching("index.html") {
        expand(
            "OIDC_DISCOVERY_URI" to localProps.getProperty("oidc.discoveryUri"),
            "OIDC_CLIENT_ID" to localProps.getProperty("oidc.clientId"),
            "OIDC_SCOPE" to localProps.getProperty("oidc.scope"),
        )
    }
}

android {
    namespace = "io.github.fgrutsch.cookmaid"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "io.github.fgrutsch.cookmaid"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        addManifestPlaceholders(mapOf("oidcRedirectScheme" to "cookmaid"))
    }
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8081\"")
            buildConfigField("String", "OIDC_DISCOVERY_URI", "\"http://10.0.2.2:1411/.well-known/openid-configuration\"")
            buildConfigField("String", "OIDC_CLIENT_ID", "\"\"")
            buildConfigField("String", "OIDC_SCOPE", "\"openid profile email offline_access\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.cookmaid.io\"")
            buildConfigField("String", "OIDC_DISCOVERY_URI", "\"\"")
            buildConfigField("String", "OIDC_CLIENT_ID", "\"\"")
            buildConfigField("String", "OIDC_SCOPE", "\"openid profile email offline_access\"")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

