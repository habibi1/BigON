pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BigON"
include(":sinema")

// Portable core (future commonMain)
include(":core:common")
include(":tmdb:model")
include(":core:network")
include(":tmdb:database")
include(":core:datastore")
include(":core:tracker:api")
include(":core:config:api")
include(":core:update")

// Compose core (CMP-ready)
include(":core:designsystem")
include(":core:ui")

// Clean architecture layers
include(":tmdb:domain")
include(":tmdb:data")
include(":tmdb:ui")
