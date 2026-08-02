plugins {
    id("convention.android-library")
    id("convention.android-hilt")
}

android {
    namespace = "com.bigon.core.database"
}

dependencies {
    api(project(":core:common"))
    api(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
}
