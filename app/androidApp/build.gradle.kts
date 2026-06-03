import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

private val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}

private val devLocalProps = Properties().apply {
    rootProject.file("dev/local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}

private val keystoreProps = Properties().apply {
    project.file("keystore.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}

android {
    namespace = "io.github.fgrutsch.cookmaid"
    compileSdk = libs.versions.android.targetCompileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "io.github.fgrutsch.cookmaid"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetCompileSdk.get().toInt()
        val parts = project.version.toString().split("-")[0].split(".")
        versionCode = parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
        versionName = project.version.toString()
        addManifestPlaceholders(mapOf("oidcRedirectScheme" to "cookmaid"))
    }
    signingConfigs {
        create("release") {
            val keystorePropsFile = rootProject.file("keystore.properties")
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", """"http://localhost:8081"""")
            buildConfigField("String", "OIDC_DISCOVERY_URI", """"${devLocalProps.getProperty("oidc.discoveryUri")}"""")
            buildConfigField("String", "OIDC_CLIENT_ID", """"${devLocalProps.getProperty("oidc.clientId")}"""")
            buildConfigField("String", "OIDC_SCOPE", """"${devLocalProps.getProperty("oidc.scope")}"""")
            buildConfigField("String", "OIDC_ACCOUNT_URI", """"${devLocalProps.getProperty("oidc.accountUri")}"""")
            buildConfigField("String", "OIDC_RESOURCE", """"${devLocalProps.getProperty("oidc.resource") ?: ""}"""")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", """"https://cookmaid.fgrutsch.dev"""")
            buildConfigField("String", "OIDC_DISCOVERY_URI", """"https://idp.fgrutsch.dev/.well-known/openid-configuration"""")
            buildConfigField("String", "OIDC_CLIENT_ID", """"4b0e486c-0dd2-40f4-8f5b-98a4ec815686"""")
            buildConfigField("String", "OIDC_SCOPE", """"openid profile email offline_access"""")
            buildConfigField("String", "OIDC_ACCOUNT_URI", """"https://idp.fgrutsch.dev/settings"""")
            buildConfigField("String", "OIDC_RESOURCE", """""""")
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
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
    }
}

tasks.register<Exec>("adbReverse") {
    val adb = "${localProps.getProperty("sdk.dir")}/platform-tools/adb"
    commandLine("sh", "-c", "$adb reverse tcp:8081 tcp:8081 && $adb reverse tcp:3001 tcp:3001")
    isIgnoreExitValue = true
}

tasks.matching { it.name.startsWith("installDev") }.configureEach {
    finalizedBy("adbReverse")
}

dependencies {
    implementation(projects.app.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.oidc)
}
