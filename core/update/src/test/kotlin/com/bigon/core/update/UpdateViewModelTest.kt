package com.bigon.core.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateViewModelTest {

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

        var flexibleAttempts = 0
        var completeCalls = 0
        val progress = MutableStateFlow<InstallProgress>(InstallProgress.Idle)

        override fun startFlexibleFlow(
            launcher: ActivityResultLauncher<IntentSenderRequest>,
        ): Boolean {
            flexibleAttempts++
            return true
        }

        override val installState: Flow<InstallProgress> get() = progress

        override fun completeUpdate() {
            completeCalls++
        }
    }

    private val critical = UpdateStatus(
        availability = UpdateAvailability.Available,
        versionCode = 7,
        priority = 5,
        immediateAllowed = true,
    )

    private val nothing = UpdateStatus(availability = UpdateAvailability.None)

    private class FakeStore(
        override var wasBlocking: Boolean = false,
        override var nagRecord: NagRecord = NagRecord(),
    ) : UpdateGateStore

    /** Available, unremarkable, and eligible for the flexible flow. */
    private val optional = UpdateStatus(
        availability = UpdateAvailability.Available,
        versionCode = 9,
        priority = 1,
        immediateAllowed = true,
        flexibleAllowed = true,
    )

    private fun controller(source: FakeSource, store: UpdateGateStore = FakeStore()) =
        UpdateViewModel(source, store, UpdateConfig())

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
    fun `a build blocked last time starts blocked, before any check runs`() = runTest {
        // Closes the window where the app is live while Play is still being
        // asked — measured at one real request going out before the gate rose.
        val controller = controller(FakeSource(nothing), FakeStore(wasBlocking = true))

        assertTrue(
            controller.state.value.isBlocking,
            "the gate should be up from construction, not after the first check",
        )
    }

    @Test
    fun `a restored gate still hands off to Play by itself`() = runTest {
        // The restored state is already blocking, so "first time we block" is
        // the wrong trigger for the hand-off — it would never fire and the user
        // would have to press the button to be told they have no choice.
        val controller = controller(FakeSource(critical), FakeStore(wasBlocking = true))

        controller.refresh()

        assertTrue(controller.consumeAutoStart())
    }

    @Test
    fun `the remembered answer is cleared once the block lifts`() = runTest {
        val store = FakeStore(wasBlocking = true)
        val controller = controller(FakeSource(nothing), store)

        controller.refresh()

        assertFalse(store.wasBlocking, "a stale block would gate the next launch for nothing")
        assertFalse(controller.state.value.isBlocking)
    }

    @Test
    fun `a decided block is remembered for the next launch`() = runTest {
        val store = FakeStore()
        val controller = controller(FakeSource(critical), store)

        controller.refresh()

        assertTrue(store.wasBlocking)
    }

    @Test
    fun `nothing available never blocks`() = runTest {
        val controller = controller(FakeSource(nothing))

        controller.refresh()

        assertFalse(controller.state.value.isBlocking)
        assertFalse(controller.consumeAutoStart())
    }

    @Test
    fun `an ordinary update suggests rather than blocks`() = runTest {
        val vm = controller(FakeSource(optional))
        vm.refresh()

        assertFalse(vm.state.value.isBlocking, "a priority-1 update blocked the app")
        assertTrue(vm.state.value.showOptionalPrompt)
    }

    @Test
    fun `an update Play will not run flexibly is not suggested`() = runTest {
        // Nothing to offer: the prompt's only button could not do anything.
        val vm = controller(FakeSource(optional.copy(flexibleAllowed = false)))
        vm.refresh()

        assertFalse(vm.state.value.showOptionalPrompt)
    }

    @Test
    fun `not now is recorded, so the next launch knows`() = runTest {
        val store = FakeStore()
        val vm = controller(FakeSource(optional), store)
        vm.refresh()

        vm.dismissOptional()

        assertFalse(vm.state.value.showOptionalPrompt)
        assertEquals(1, store.nagRecord.dismissals, "the refusal was not counted")
        assertEquals(optional.versionCode, store.nagRecord.versionCode)
    }

    @Test
    fun `a version already refused three times is not raised again`() = runTest {
        val store = FakeStore(
            nagRecord = NagRecord(
                versionCode = optional.versionCode,
                dismissals = 3,
                launchesSinceDismissal = 99,
            ),
        )
        val vm = controller(FakeSource(optional), store)
        vm.refresh()

        assertFalse(vm.state.value.showOptionalPrompt, "kept asking past the dismissal budget")
    }

    @Test
    fun `the prompt is raised once per process, not once per resume`() = runTest {
        // Every foreground triggers a check. Without this guard, switching apps
        // reopens the sheet on top of whatever the user came back to do.
        val vm = controller(FakeSource(optional))
        vm.refresh()
        vm.dismissOptional()

        vm.refresh()

        assertFalse(vm.state.value.showOptionalPrompt, "the sheet returned on the next resume")
    }

    @Test
    fun `accepting starts the flexible flow and closes the sheet immediately`() = runTest {
        // Immediately, because the download runs in the background — waiting for
        // Play would leave the sheet sitting over an app that is already free.
        val source = FakeSource(optional)
        val vm = controller(source)
        vm.refresh()

        vm.acceptOptional(launcher)

        assertEquals(1, source.flexibleAttempts)
        assertEquals(0, source.startAttempts, "started the blocking flow for an optional update")
        assertFalse(vm.state.value.showOptionalPrompt)
    }

    @Test
    fun `a forced update takes over from a suggestion`() = runTest {
        val source = FakeSource(optional)
        val vm = controller(source)
        vm.refresh()
        assertTrue(vm.state.value.showOptionalPrompt)

        // Promoted while the app was open — the sheet has nothing left to offer.
        source.status = critical
        vm.refresh()

        assertTrue(vm.state.value.isBlocking)
        assertFalse(vm.state.value.showOptionalPrompt)
    }

    @Test
    fun `restarting is delegated to Play`() = runTest {
        val source = FakeSource(optional)
        val vm = controller(source)

        vm.completeUpdate()

        assertEquals(1, source.completeCalls)
    }

}
