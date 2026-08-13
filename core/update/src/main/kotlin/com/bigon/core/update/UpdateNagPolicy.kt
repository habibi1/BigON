package com.bigon.core.update

/**
 * How often an optional update may interrupt someone.
 *
 * Kept apart from [UpdatePolicy] because it answers a different question.
 * [UpdatePolicy] asks whether an update *matters*, from facts Play supplies.
 * This asks whether the user should be *asked again*, from a history only the
 * app has. The first is about the release; the second is about a person who
 * has already said no twice.
 *
 * The rule: ask immediately the first time a version appears, wait
 * [UpdateConfig.promptAfterLaunches] launches after each "Not now", and stop
 * entirely after [UpdateConfig.maxDismissals]. A new version resets all of it —
 * declining 1.4 says nothing about 1.5.
 */
class UpdateNagPolicy(private val config: UpdateConfig = UpdateConfig()) {

    /**
     * @param record what this install remembers about being asked.
     * @param versionCode the version Play is currently offering.
     */
    fun shouldPrompt(record: NagRecord, versionCode: Int): Boolean {
        // A version the user has never been shown is always worth one mention,
        // whatever they said about the last one.
        if (record.versionCode != versionCode) return true

        if (record.dismissals >= config.maxDismissals) return false

        return record.launchesSinceDismissal >= config.promptAfterLaunches
    }

    /** The record after the user declines [versionCode]. */
    fun onDismissed(record: NagRecord, versionCode: Int): NagRecord =
        if (record.versionCode != versionCode) {
            // First refusal of a new version: start its count, not continue the
            // previous version's.
            NagRecord(versionCode = versionCode, dismissals = 1, launchesSinceDismissal = 0)
        } else {
            record.copy(dismissals = record.dismissals + 1, launchesSinceDismissal = 0)
        }

    /**
     * The record after the app is launched. Only counts once the user has
     * actually declined something — counting launches before that would let a
     * frequently-opened app burn through its budget before ever asking.
     */
    fun onLaunched(record: NagRecord): NagRecord =
        if (record.dismissals == 0) {
            record
        } else {
            record.copy(launchesSinceDismissal = record.launchesSinceDismissal + 1)
        }
}

/**
 * What one install remembers about being nagged. Scoped to a version code: the
 * counters mean nothing without knowing which release they were about.
 */
data class NagRecord(
    val versionCode: Int = 0,
    val dismissals: Int = 0,
    val launchesSinceDismissal: Int = 0,
)
