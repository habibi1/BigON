plugins {
    id("convention.android-library")
}

android {
    namespace = "com.bigon.core.update"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
