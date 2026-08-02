plugins {
    id("convention.jvm-library")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.javax.inject)
    api(project(":core:common"))
}
