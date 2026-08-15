plugins {
    id("convention.android-library")
    id("convention.android-hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bigon.tmdb.data"
}

dependencies {
    api(project(":tmdb:domain"))
    implementation(project(":core:common"))
    implementation(project(":tmdb:model"))
    implementation(project(":core:network"))
    implementation(project(":tmdb:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:config:api"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
