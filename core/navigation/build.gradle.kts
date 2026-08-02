plugins {
    id("convention.jvm-library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Route types are @Serializable so Navigation Compose can carry them
    // type-safely; no Compose or Android dependency belongs in this module.
    implementation(libs.kotlinx.serialization.json)
}
