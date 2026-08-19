package com.bigon.sinema

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * §10 — Konsist architecture tests: rules that aren't enforced decay.
 */
class ArchitectureTest {

    /** Framework-free JVM modules — business logic and contracts. */
    private val jvmModulePackagePrefixes = listOf(
        "com.bigon.core.common",
        "com.bigon.tmdb.model",
        "com.bigon.core.network",
        "com.bigon.core.tracker",
        "com.bigon.core.config",
        "com.bigon.sinema.navigation",
        "com.bigon.tmdb.domain",
    )

    /** Modules allowed to touch Context — the app shell and platform adapters. */
    private val contextAllowedPackagePrefixes = listOf(
        // The app shell (composition root) and the platform adapter modules.
        "com.bigon.sinema",
        "com.bigon.tmdb.database",
        "com.bigon.core.datastore",
        // Adapts Play's in-app update API, which is Context-bound at its root.
        "com.bigon.core.update",
        "com.bigon.tmdb.data",
    )

    /**
     * Konsist walks the project directory from disk, and a git worktree created
     * under `.claude/` is a second, complete copy of every module parked on
     * whatever commit that task started from. Without this filter every rule is
     * also asserted against that stale copy — so a package move in the real tree
     * "fails" because the copy has not moved with it, and the failure names files
     * at paths that look right until you read them closely.
     */
    private fun productionFiles() = Konsist.scopeFromProduction()
        .files
        .filterNot { "/.claude/" in it.path }

    private fun filesIn(prefixes: List<String>) = productionFiles().filter { file ->
        val pkg = file.packagee?.name ?: return@filter false
        prefixes.any { pkg == it || pkg.startsWith("$it.") }
    }

    @Test
    fun `jvm modules do not import android APIs`() {
        filesIn(jvmModulePackagePrefixes).assertFalse { file ->
            file.imports.any { import ->
                import.name.startsWith("android.") || import.name.startsWith("androidx.")
            }
        }
    }

    @Test
    fun `Context stays inside the app shell and platform adapter modules`() {
        productionFiles()
            .filter { file ->
                file.imports.any { it.name == "android.content.Context" }
            }
            .assertTrue { file ->
                val pkg = file.packagee?.name.orEmpty()
                contextAllowedPackagePrefixes.any { pkg == it || pkg.startsWith("$it.") }
            }
    }

    @Test
    fun `domain depends only on the whitelist`() {
        productionFiles()
            .filter { it.packagee?.name?.startsWith("com.bigon.tmdb.domain") == true }
            .assertTrue { file ->
                file.imports.all { import ->
                    import.name.startsWith("kotlin") ||          // kotlin.*, kotlinx.*
                        import.name.startsWith("com.bigon.tmdb.model") ||
                        import.name.startsWith("com.bigon.core.common") ||
                        import.name.startsWith("javax.inject") ||
                        import.name.startsWith("jakarta.inject")
                }
            }
    }

    /**
     * The navigation host stays swappable (ADR-011): destinations are plain
     * `@Serializable` values in :core:navigation, and only the shell's own
     * navigation plumbing may see androidx.navigation. A screen that took a
     * NavController would pin the app to this host — exactly the coupling the
     * CMP migration has to avoid. Screens raise callbacks instead.
     *
     * `androidx.hilt.navigation.compose` is DI plumbing, not the host, so it is
     * deliberately outside this rule.
     */
    @Test
    fun `only the app shell touches the navigation host`() {
        productionFiles()
            .filter { file ->
                file.imports.any { it.name.startsWith("androidx.navigation.") }
            }
            .assertTrue { file ->
                file.packagee?.name in setOf("com.bigon.sinema.ui", "com.bigon.sinema.navigation")
            }
    }

    /**
     * Play's in-app update API stays inside :core:update, for the same reason
     * the navigation host stays inside the shell. "Is this build still allowed
     * to run?" has to have exactly one answer; a second module starting its own
     * update flow would split that decision in two, and the half that says yes
     * wins by default.
     */
    @Test
    fun `only the update module touches Play's update API`() {
        productionFiles()
            .filter { file ->
                file.imports.any { it.name.startsWith("com.google.android.play.core.") }
            }
            .assertTrue { file ->
                file.packagee?.name == "com.bigon.core.update"
            }
    }

    @Test
    fun `banned stacks never appear`() {
        productionFiles().assertFalse { file ->
            file.imports.any { import ->
                import.name.startsWith("com.squareup.moshi.") ||
                    import.name.startsWith("com.google.gson.") ||
                    import.name.startsWith("io.reactivex.") ||
                    import.name.startsWith("org.koin.")
            }
        }
    }

    /**
     * The reason core/ exists: a module in it must be usable by an app that has
     * never heard of Sinema or TMDB. That is not a property review can hold —
     * one convenient import of a Movie, and "core" quietly becomes "Sinema's
     * shared code", which is what it had already become before this rule.
     *
     * Three tiers, and dependencies only ever point up:
     *   core/  domain-agnostic   — any app
     *   tmdb/  the TMDB domain   — any movie app
     *   app    everything else
     */
    @Test
    fun `core modules know nothing about the app or its domain`() {
        productionFiles()
            .filter { it.packagee?.name?.startsWith("com.bigon.core") == true }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("com.bigon.tmdb") ||
                        import.name.startsWith("com.bigon.sinema")
                } || file.name.contains("Sinema")
            }
    }

    /**
     * The tmdb/ tier may use core/, but never the app: a second movie app has
     * its own screens and must be able to take this layer without them.
     */
    @Test
    fun `the tmdb layer does not depend on the app`() {
        productionFiles()
            .filter { it.packagee?.name?.startsWith("com.bigon.tmdb") == true }
            .assertFalse { file ->
                file.imports.any { it.name.startsWith("com.bigon.sinema") }
            }
    }
}
