plugins {
    id("convention.android-library")
    id("convention.android-hilt")
}

android {
    namespace = "com.bigon.core.datastore"
}

dependencies {
    api(project(":core:common"))
    api(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
