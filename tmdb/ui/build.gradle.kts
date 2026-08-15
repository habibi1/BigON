plugins {
    id("convention.android-library")
    id("convention.compose")
}

android {
    namespace = "com.bigon.tmdb.ui"
}

dependencies {
    // The shared visual language; these components are TMDB-shaped, not
    // differently-styled.
    api(project(":core:designsystem"))
}
