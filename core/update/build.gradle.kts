plugins {
    id("convention.android-library")
    id("convention.android-hilt")
    id("convention.compose")
}

android {
    namespace = "com.bigon.core.update"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // Play refuses an immediate update against a sideloaded build, so
            // the flow is otherwise unverifiable until it is already in front of
            // users. These drive Google's FakeAppUpdateManager (see the debug
            // source set) and belong to this module, not to the host app —
            // rehearsing a force update should not cost the app any code:
            //
            //   ./gradlew installDebug -Pbigon.fakeUpdatePriority=5
            //
            // Absent the property the fake is never constructed at all.
            buildConfigField("int", "FAKE_UPDATE_PRIORITY", fakeUpdateProperty("bigon.fakeUpdatePriority", "-1"))
            buildConfigField("int", "FAKE_UPDATE_STALENESS_DAYS", fakeUpdateProperty("bigon.fakeUpdateStalenessDays", "0"))
        }
    }
}

/** Reads a rehearsal knob from -P or gradle.properties, falling back to off. */
fun fakeUpdateProperty(name: String, fallback: String): String =
    (providers.gradleProperty(name).orNull ?: fallback).trim()

dependencies {
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(libs.kotlinx.coroutines.core)

    // The host Activity is what Play's IntentSender flow needs, and the gate
    // registers its own launcher so the app does not have to.
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.compose.material.icons)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
