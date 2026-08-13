package com.bigon.core.update

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdatePolicyTest {

    private val policy = UpdatePolicy()

    private fun available(
        priority: Int = 0,
        stalenessDays: Int? = null,
        immediateAllowed: Boolean = true,
    ) = UpdateStatus(
        availability = UpdateAvailability.Available,
        versionCode = 42,
        priority = priority,
        stalenessDays = stalenessDays,
        immediateAllowed = immediateAllowed,
    )

    @Test
    fun `nothing available lets the app run`() {
        assertEquals(
            UpdateDecision.Continue,
            policy.decide(UpdateStatus(availability = UpdateAvailability.None)),
        )
    }

    @Test
    fun `a routine update is not forced`() {
        // The common case, and the one worth protecting: shipping a patch must
        // not lock out everyone on the previous build.
        assertEquals(UpdateDecision.Continue, policy.decide(available(priority = 0)))
        assertEquals(UpdateDecision.Continue, policy.decide(available(priority = 3)))
    }

    @Test
    fun `priority five forces immediately`() {
        val decision = policy.decide(available(priority = 5))

        assertIs<UpdateDecision.Force>(decision)
        assertEquals(ForceReason.Priority, decision.reason)
        assertEquals(42, decision.versionCode)
    }

    @Test
    fun `priority five ignores staleness entirely`() {
        // Critical means now, not "now, once Play has been offering it a while".
        val decision = policy.decide(available(priority = 5, stalenessDays = 0))

        assertIs<UpdateDecision.Force>(decision)
    }

    @Test
    fun `priority four waits out its grace period`() {
        assertEquals(
            UpdateDecision.Continue,
            policy.decide(available(priority = 4, stalenessDays = 6)),
        )

        val decision = policy.decide(available(priority = 4, stalenessDays = 7))
        assertIs<UpdateDecision.Force>(decision)
        assertEquals(ForceReason.Staleness, decision.reason)
    }

    @Test
    fun `unknown staleness is treated as fresh`() {
        // Play returns null until it knows. Reading that as "very stale" would
        // force an update the moment a high-priority release appeared, which is
        // the opposite of what the grace period is for.
        assertEquals(
            UpdateDecision.Continue,
            policy.decide(available(priority = 4, stalenessDays = null)),
        )
    }

    @Test
    fun `an update Play will not install immediately never blocks`() {
        // Sideloads and stale Play Stores report the update but refuse the
        // flow. Blocking here would strand the app with no way forward — the
        // single worst outcome this policy can produce.
        assertEquals(
            UpdateDecision.Continue,
            policy.decide(available(priority = 5, immediateAllowed = false)),
        )
    }

    @Test
    fun `an interrupted update resumes regardless of priority`() {
        val decision = policy.decide(
            UpdateStatus(
                availability = UpdateAvailability.InProgress,
                versionCode = 42,
                priority = 0,
                immediateAllowed = true,
            ),
        )

        assertIs<UpdateDecision.Force>(decision)
        assertEquals(ForceReason.Resume, decision.reason)
    }

    @Test
    fun `an interrupted update that can no longer run lets the app through`() {
        assertEquals(
            UpdateDecision.Continue,
            policy.decide(
                UpdateStatus(
                    availability = UpdateAvailability.InProgress,
                    immediateAllowed = false,
                ),
            ),
        )
    }
}
