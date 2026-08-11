package com.bigon.core.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForceUpdateViewModelTest {

    private class FakeSource(var status: UpdateStatus) : UpdateSource {
        var startAttempts = 0
        var startSucceeds = true
        var cancelNextCheck = false

        override suspend fun check(): UpdateStatus {
            if (cancelNextCheck) throw CancellationException("backgrounded mid-check")
            return status
        }

        override fun startImmediateFlow(
            launcher: ActivityResultLauncher<IntentSenderRequest>,
        ): Boolean {
            startAttempts++
            return startSucceeds
        }
    }

    private val critical = UpdateStatus(
        availability = UpdateAvailability.Available,
        versionCode = 7,
        priority = 5,
        immediateAllowed = true,
    )

    private val nothing = UpdateStatus(availability = UpdateAvailability.None)

    private fun controller(source: FakeSource) = ForceUpdateViewModel(source, UpdateConfig())

    /**
     * Inert launcher: the controller only ever passes it through to the source,
     * which here is a fake, so nothing is ever launched.
     */
    private val launcher = object : ActivityResultLauncher<IntentSenderRequest>() {
        override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
        override val contract = ActivityResultContracts.StartIntentSenderForResult()
    }

    @Test
    fun `a critical update raises the gate`() = runTest {
        val controller = controller(FakeSource(critical))

        controller.refresh()

        val state = controller.state.value
        assertTrue(state.isBlocking)
        assertEquals(ForceReason.Priority, state.reason)
        assertEquals(7, state.versionCode)
    }

    @Test
    fun `the hand-off to Play happens once, not on every resume`() = runTest {
        // The loop this prevents: resume checks, the check opens Play, backing
        // out of Play resumes again. Left alone that pins the user between two
        // screens with no way to read either.
        val controller = controller(FakeSource(critical))

        controller.refresh()
        assertTrue(controller.consumeAutoStart(), "first check should hand off")

        controller.refresh()
        assertFalse(controller.consumeAutoStart(), "a later resume must not hand off again")
    }

    @Test
    fun `the gate survives the user backing out of Play`() = runTest {
        val source = FakeSource(critical)
        val controller = controller(source)
        controller.refresh()

        controller.onPlayFlowResult()
        controller.refresh()

        assertTrue(controller.state.value.isBlocking, "declining must not unlock the app")
        assertFalse(controller.state.value.isPlayFlowVisible)
    }

    @Test
    fun `the gate comes back down if the update stops being required`() = runTest {
        // A release pulled from Play, or a policy that no longer forces: the
        // block must not latch for the life of the process.
        val source = FakeSource(critical)
        val controller = controller(source)
        controller.refresh()
        assertTrue(controller.state.value.isBlocking)

        source.status = nothing
        controller.refresh()

        assertFalse(controller.state.value.isBlocking)
    }

    @Test
    fun `a failed hand-off leaves the gate up rather than the app open`() = runTest {
        val source = FakeSource(critical).apply { startSucceeds = false }
        val controller = controller(source)
        controller.refresh()

        controller.startUpdate(launcher)

        assertTrue(controller.state.value.isBlocking)
        assertFalse(
            controller.state.value.isPlayFlowVisible,
            "nothing opened, so the gate must show its own retry",
        )
    }

    @Test
    fun `a cancelled check leaves the gate exactly as it was`() = runTest {
        // The app being backgrounded mid-check must not be read as "no update".
        // If it is, every resume reopens a blocked app for as long as the next
        // check takes.
        val source = FakeSource(critical)
        val controller = controller(source)
        controller.refresh()
        assertTrue(controller.state.value.isBlocking)

        source.cancelNextCheck = true
        assertFailsWith<CancellationException> { controller.refresh() }

        assertTrue(controller.state.value.isBlocking, "cancellation must not lower the gate")
    }

    @Test
    fun `a check that cannot reach Play unblocks, by design`() = runTest {
        // Deliberate and worth pinning: a device that permanently cannot talk to
        // Play would otherwise be stranded on the gate with no way forward. The
        // source reports failures as "nothing available" for exactly this reason.
        val source = FakeSource(critical)
        val controller = controller(source)
        controller.refresh()
        assertTrue(controller.state.value.isBlocking)

        source.status = nothing
        controller.refresh()

        assertFalse(controller.state.value.isBlocking)
    }

    @Test
    fun `nothing available never blocks`() = runTest {
        val controller = controller(FakeSource(nothing))

        controller.refresh()

        assertFalse(controller.state.value.isBlocking)
        assertFalse(controller.consumeAutoStart())
    }
}
