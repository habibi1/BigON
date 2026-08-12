package com.bigon.core.update

/**
 * What Play knows about an available update, with Play's types left at the
 * door. Keeping this framework-free is what lets the "should we force?"
 * question be answered in a unit test rather than only against a live Play
 * Store — the one part of in-app updates that is genuinely ours to get right.
 */
data class UpdateStatus(
    val availability: UpdateAvailability,
    /** The version waiting on Play, or 0 when nothing is. */
    val versionCode: Int = 0,
    /**
     * 0..5, set per-release through the Play Developer API — never in the app.
     * A build cannot know how urgent it is; only the release that supersedes it
     * can say. That is the whole reason this value comes down the wire.
     */
    val priority: Int = 0,
    /** Days since Play first offered this update, or null while Play is unsure. */
    val stalenessDays: Int? = null,
    /** Play refuses IMMEDIATE for some install sources; then we cannot force. */
    val immediateAllowed: Boolean = false,
)

enum class UpdateAvailability {
    /** Nothing newer, or Play could not say. */
    None,

    /** A newer version is on Play and has not been started. */
    Available,

    /**
     * An immediate update was already accepted and then interrupted — the user
     * backgrounded the app mid-download. Play requires this be resumed.
     */
    InProgress,
}

sealed interface UpdateDecision {
    /** Let the app run. */
    data object Continue : UpdateDecision

    /** Block the app and run Play's immediate flow. */
    data class Force(val versionCode: Int, val reason: ForceReason) : UpdateDecision

}

enum class ForceReason {
    /** Priority 5: the release itself declared it un-skippable. */
    Priority,

    /** High priority that has been available long enough to stop asking nicely. */
    Staleness,

    /** Finishing what the user already accepted. */
    Resume,
}

/**
 * Decides when an available update stops being optional.
 *
 * The rule is deliberately conservative. Forcing on *every* update would mean a
 * typo fix in a string locks out everyone on the previous build until they sit
 * through a download — so the default answer is no, and a release has to ask to
 * be treated as urgent by carrying a priority.
 *
 * With the default [UpdateConfig]:
 *
 * | priority | staleness      | result   |
 * |----------|----------------|----------|
 * | 5        | any            | force    |
 * | 4        | >= 7 days      | force    |
 * | 4        | < 7 days       | continue |
 * | 0..3     | any            | continue |
 *
 * Priority 4 gets the grace period because it is the "important, not on fire"
 * level: most people update on their own within a week, and the ones who
 * haven't are the ones worth interrupting.
 */
data class UpdateConfig(
    /** Priority that forces with no grace period at all. */
    val criticalPriority: Int = 5,
    /** Priority that forces only once [gracePeriodDays] have passed. */
    val highPriority: Int = 4,
    val gracePeriodDays: Int = 7,
    /**
     * Launches to wait before asking again after a "Not now". The first ask is
     * immediate; only re-asks wait.
     */
    val promptAfterLaunches: Int = 3,
    /**
     * How many times one version may be declined before the app stops asking.
     * A prompt that returns every time trains people to dismiss it on sight,
     * which costs the one prompt that will matter later.
     */
    val maxDismissals: Int = 3,
) {
    init {
        // A zero threshold turns "urgent releases only" into "every release",
        // which is the outcome this policy exists to prevent — and it would do
        // it silently, on everyone, at the next routine patch. Cheaper to fail
        // at construction than to work out afterwards why the whole user base
        // was locked out by a typo fix.
        require(criticalPriority in 1..MAX_PLAY_PRIORITY) {
            "criticalPriority must be 1..$MAX_PLAY_PRIORITY, was $criticalPriority"
        }
        require(highPriority in 1..criticalPriority) {
            "highPriority must be 1..criticalPriority ($criticalPriority), was $highPriority"
        }
        require(gracePeriodDays >= 0) {
            "gracePeriodDays cannot be negative, was $gracePeriodDays"
        }
        require(promptAfterLaunches >= 0) {
            "promptAfterLaunches cannot be negative, was $promptAfterLaunches"
        }
        require(maxDismissals >= 0) {
            "maxDismissals cannot be negative, was $maxDismissals"
        }
    }

    private companion object {
        /** Play's own ceiling for updatePriority. */
        const val MAX_PLAY_PRIORITY = 5
    }
}

class UpdatePolicy(private val config: UpdateConfig = UpdateConfig()) {

    fun decide(status: UpdateStatus): UpdateDecision = when (status.availability) {
        // An interrupted immediate update is resumed before any policy question
        // is asked. The user already agreed; this is finishing, not deciding.
        // If Play has since withdrawn IMMEDIATE we let the app run rather than
        // strand it on a flow that cannot start.
        UpdateAvailability.InProgress ->
            if (status.immediateAllowed) {
                UpdateDecision.Force(status.versionCode, ForceReason.Resume)
            } else {
                UpdateDecision.Continue
            }

        UpdateAvailability.None -> UpdateDecision.Continue

        UpdateAvailability.Available -> when {
            // Sideloaded builds, some enterprise installs, and any device
            // without a current Play Store report the update but disallow the
            // flow. Blocking there would brick the app with no way forward.
            !status.immediateAllowed -> UpdateDecision.Continue

            status.priority >= config.criticalPriority ->
                UpdateDecision.Force(status.versionCode, ForceReason.Priority)

            status.priority >= config.highPriority &&
                (status.stalenessDays ?: 0) >= config.gracePeriodDays ->
                UpdateDecision.Force(status.versionCode, ForceReason.Staleness)

            else -> UpdateDecision.Continue
        }
    }
}
