package com.bigon.core.update

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The nag policy decides how often someone may be interrupted about an update
 * they are allowed to ignore. Getting it wrong is not a crash — it is an app
 * that either nags until the prompt is reflex-dismissed, or goes quiet forever
 * after one "not now".
 */
class UpdateNagPolicyTest {

    private val policy = UpdateNagPolicy()
    private val version = 42

    @Test
    fun `a version never seen before is always worth one mention`() {
        // Even from a record that exhausted its budget on the previous release:
        // declining 1.4 says nothing about 1.5.
        val exhausted = NagRecord(versionCode = 41, dismissals = 99, launchesSinceDismissal = 0)
        assertTrue(policy.shouldPrompt(exhausted, version))
    }

    @Test
    fun `not now silences the prompt until enough launches have passed`() {
        var record = policy.onDismissed(NagRecord(), version)
        assertFalse(policy.shouldPrompt(record, version), "asked again immediately after a refusal")

        repeat(2) { record = policy.onLaunched(record) }
        assertFalse(policy.shouldPrompt(record, version), "asked again after only 2 of 3 launches")

        record = policy.onLaunched(record)
        assertTrue(policy.shouldPrompt(record, version), "never asked again")
    }

    @Test
    fun `three refusals of one version end the conversation`() {
        var record = NagRecord()
        repeat(3) {
            record = policy.onDismissed(record, version)
            repeat(5) { record = policy.onLaunched(record) }
        }

        assertFalse(
            policy.shouldPrompt(record, version),
            "still asking after 3 refusals — this is the behaviour that trains people to dismiss on sight",
        )
    }

    @Test
    fun `a new version resets a used-up budget`() {
        var record = NagRecord()
        repeat(3) { record = policy.onDismissed(record, version) }
        assertFalse(policy.shouldPrompt(record, version))

        assertTrue(policy.shouldPrompt(record, version + 1))
    }

    @Test
    fun `launches before any refusal are not counted`() {
        // Otherwise a frequently-opened app spends its budget before it has
        // asked anything, and the first prompt lands already half-silenced.
        var record = NagRecord()
        repeat(10) { record = policy.onLaunched(record) }

        assertEquals(0, record.launchesSinceDismissal)
    }

    @Test
    fun `refusing a different version starts that version's count at one`() {
        val old = NagRecord(versionCode = 41, dismissals = 2, launchesSinceDismissal = 7)

        val fresh = policy.onDismissed(old, version)

        assertEquals(version, fresh.versionCode)
        assertEquals(1, fresh.dismissals, "carried the previous version's count forward")
        assertEquals(0, fresh.launchesSinceDismissal)
    }

    @Test
    fun `a zero-launch config asks again at the next opportunity`() {
        val eager = UpdateNagPolicy(UpdateConfig(promptAfterLaunches = 0))
        val record = eager.onDismissed(NagRecord(), version)

        assertTrue(eager.shouldPrompt(record, version))
    }

    @Test
    fun `a zero-dismissal config never asks twice`() {
        val once = UpdateNagPolicy(UpdateConfig(maxDismissals = 0))
        val record = once.onDismissed(NagRecord(), version)

        assertFalse(once.shouldPrompt(record, version))
    }
}
