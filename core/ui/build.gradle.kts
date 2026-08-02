plugins {
    id("convention.android-library")
    id("convention.compose")
}

android {
    namespace = "com.bigon.core.ui"
}

dependencies {
    api(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
}
