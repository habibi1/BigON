plugins {
    id("convention.jvm-library")
}

dependencies {
    api(project(":tmdb:model"))
    implementation(libs.javax.inject)
    api(project(":core:common"))
}
