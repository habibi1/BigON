plugins {
    `kotlin-dsl`
}

group = "com.bigon.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "convention.android-library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "convention.jvm-library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = "convention.android-hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("compose") {
            id = "convention.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("feature") {
            id = "convention.feature"
            implementationClass = "FeatureConventionPlugin"
        }
    }
}
