import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("convention.android-hilt")
}

// Secrets stay out of the repo: TMDB_API_KEY is read from local.properties.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val tmdbApiKey: String = localProperties.getProperty("TMDB_API_KEY").orEmpty()
val tmdbReadAccessToken: String = localProperties.getProperty("TMDB_READ_ACCESS_TOKEN").orEmpty()
if (tmdbApiKey.isBlank() && tmdbReadAccessToken.isBlank()) {
    logger.warn(
        "TMDB credentials missing: add TMDB_API_KEY or TMDB_READ_ACCESS_TOKEN to local.properties. " +
            "The app will build, but every request will return 401.",
    )
}

android {
    namespace = "com.bigon.sinema"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.bigon.sinema"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.0.1"

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"$tmdbReadAccessToken\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Clean architecture graph — :app is the only composition root (§3.3).
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:tracker:api"))
    implementation(project(":core:config:api"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":data"))
    implementation(project(":domain"))

    // Platform adapters (the only module allowed to know these exist)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    // Compose (100% Compose UI — §1)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation.compose)

    // Image loading — shares the app's OkHttp client (connection pool + timeouts)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.konsist)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
