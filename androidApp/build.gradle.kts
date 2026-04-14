import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

private val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
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
        val parts = project.version.toString().split("-")[0].split(".")
        versionCode = parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
        versionName = project.version.toString()
        addManifestPlaceholders(mapOf("oidcRedirectScheme" to "cookmaid"))
    }
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", """"http://localhost:8081"""")
            buildConfigField("String", "OIDC_DISCOVERY_URI", """"${localProps.getProperty("oidc.discoveryUri")}"""")
            buildConfigField("String", "OIDC_CLIENT_ID", """"${localProps.getProperty("oidc.clientId")}"""")
            buildConfigField("String", "OIDC_SCOPE", """"${localProps.getProperty("oidc.scope")}"""")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", """"https://cookmaid.fgrutsch.dev"""")
            buildConfigField("String", "OIDC_DISCOVERY_URI", """"https://idp.fgrutsch.dev/.well-known/openid-configuration"""")
            buildConfigField("String", "OIDC_CLIENT_ID", """"4b0e486c-0dd2-40f4-8f5b-98a4ec815686"""")
            buildConfigField("String", "OIDC_SCOPE", """"openid profile email offline_access"""")
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

tasks.register<Exec>("adbReverse") {
    val adb = "${localProps.getProperty("sdk.dir")}/platform-tools/adb"
    commandLine("sh", "-c", "$adb reverse tcp:8081 tcp:8081 && $adb reverse tcp:8082 tcp:8082")
    isIgnoreExitValue = true
}

tasks.matching { it.name.startsWith("installDev") }.configureEach {
    finalizedBy("adbReverse")
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.oidc)
}
