plugins {
    id("convention.jvm-library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:common"))

    // Exposed: repositories in :data declare Retrofit service interfaces and
    // consumers build them from the injected Retrofit instance.
    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.javax.inject)

    testImplementation(libs.okhttp.mockwebserver)
}
