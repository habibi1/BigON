import com.android.build.api.dsl.LibraryExtension
import com.bigon.buildlogic.configureAndroidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Base archetype for every Android library module: AGP + Kotlin, shared SDK levels,
 * shared JVM target. Applied directly by Compose/UI modules; portable modules apply
 * `convention.portable-library` instead, which layers the dependency guard on top.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9 ships built-in Kotlin support; applying KGP's android plugin is an error.
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                configureAndroidLibrary(this)
            }
        }
    }
}
