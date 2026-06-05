import java.util.*

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            project.file("keystore.properties").takeIf { it.exists() }
                ?.let { file -> Properties().apply { file.reader().use { load(it) } } }
                ?.let { props ->
                    storeFile = file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
        }
    }
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            val devProps = Properties().apply {
                rootProject.file("dev/local.properties").inputStream().use { load(it) }
            }
            buildConfigField("String", "BASE_URL", """"http://localhost:8081"""")
            buildConfigField("String", "OIDC_DISCOVERY_URI", """"${devProps.getProperty("oidc.discoveryUri")}"""")
            buildConfigField("String", "OIDC_CLIENT_ID", """"${devProps.getProperty("oidc.clientId")}"""")
            buildConfigField("String", "OIDC_SCOPE", """"${devProps.getProperty("oidc.scope")}"""")
            buildConfigField("String", "OIDC_ACCOUNT_URI", """"${devProps.getProperty("oidc.accountUri")}"""")
            buildConfigField("String", "OIDC_RESOURCE", """"${devProps.getProperty("oidc.resource") ?: ""}"""")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", """"https://cookmaid.fgrutsch.dev"""")
            buildConfigField(
                "String",
                "OIDC_DISCOVERY_URI",
                """"https://sso.fgrutsch.dev/oidc/.well-known/openid-configuration""""
            )
            buildConfigField("String", "OIDC_CLIENT_ID", """"n12rjceuk6vgy676z9e3y"""")
            buildConfigField("String", "OIDC_SCOPE", """"openid profile email"""")
            buildConfigField("String", "OIDC_ACCOUNT_URI", """"https://sso.fgrutsch.dev/account/security"""")
            buildConfigField("String", "OIDC_RESOURCE", """"https://cookmaid.fgrutsch.dev/api"""")
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
    description = "Reverse-forward adb ports so the emulator/device reaches the local server and Logto"
    val localProps = Properties().apply {
        rootProject.file("local.properties").inputStream().use { load(it) }
    }
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
