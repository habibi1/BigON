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
            // R8: shrink, optimise, obfuscate, and shrink resources. AGP 9's
            // `optimization` block turns all of it on and supplies Android's
            // default keep rules, so there is no getDefaultProguardFile() call
            // and no proguardFiles list to keep in sync.
            //
            // There is deliberately no keep-rule file. Every dependency that
            // needs rules ships its own — kotlinx.serialization, Retrofit,
            // OkHttp, Room, Hilt, Coil and Play all contribute to the merged
            // configuration, and R8 asked for nothing further (no
            // missing_rules.txt was produced). Nothing in this app reaches for
            // a type by name at runtime, so there is nothing left to protect.
            //
            // If that changes, rules belong in `src/main/keepRules/*.keep` in
            // the module that needs them, not in one file here: core/ and tmdb/
            // are meant to be reusable, and a second app should inherit their
            // rules the same way it inherits their code.
            optimization {
                enable = true
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
    implementation(project(":tmdb:model"))
    implementation(project(":core:network"))
    implementation(project(":tmdb:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:tracker:api"))
    implementation(project(":core:config:api"))
    implementation(project(":core:update"))
    implementation(project(":core:designsystem"))
    implementation(project(":tmdb:ui"))
    implementation(project(":core:ui"))
    implementation(project(":tmdb:data"))
    implementation(project(":tmdb:domain"))

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
    // Backports Android 12's splash screen to API 26–30, where the window would
    // otherwise be blank while the process starts.
    implementation(libs.androidx.core.splashscreen)
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
