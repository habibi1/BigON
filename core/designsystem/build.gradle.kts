plugins {
    id("convention.android-library")
    id("convention.compose")
}

android {
    namespace = "com.bigon.core.designsystem"
}

dependencies {
    implementation(libs.compose.material.icons)
}
