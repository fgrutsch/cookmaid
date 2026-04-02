plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.oidc)
}
