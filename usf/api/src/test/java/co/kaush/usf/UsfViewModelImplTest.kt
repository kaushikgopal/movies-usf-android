package co.kaush.usf


import co.kaush.usf.fixtures.TestEffect
import co.kaush.usf.fixtures.TestEvent
import co.kaush.usf.fixtures.TestViewModel
import co.kaush.usf.fixtures.TestViewState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class UsfViewModelImplTest {

    // Set the main coroutines dispatcher for unit testing.
    @OptIn(ExperimentalCoroutinesApi::class)
    @JvmField
    @RegisterExtension
    val testRule = CoroutineTestRule()

    @Test
    @DisplayName("core test: event -> result -> (view state + effect)")
    fun testBasicEmission() = runTest {
        val viewModel = createViewModel()

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent() // actually make the subscription connection

        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        assertThat(es).hasSize(1).containsExactly(TestEffect.TestEffect1)

        assertThat(vs).hasSize(2)
            .containsExactly(TestViewState("[VS] initial"), TestViewState("[VS] 1 "))
    }


    @Test
    @DisplayName("core test: events sent after some emissions processed correctly")
    fun testHotFlow() = runTest {
        val busFlow = MutableSharedFlow<Int>()
        val viewModel = createViewModel(busFlow)

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        busFlow.emit(42)
        runCurrent()

        assertThat(es).hasSize(1).containsExactly(TestEffect.TestInitFlowEffect(42))
        assertThat(vs).last().isEqualTo(TestViewState("[VS] initial", number = 42))
    }

    @Test
    @DisplayName("view state supplied during initialization is captured by first subscriber")
    fun testInitialViewState() = runTest {
        val viewModel = createViewModel(emptyFlow())

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        assertThat(es).isEmpty()
        assertThat(vs).hasSize(1).containsExactly(TestViewState("[VS] initial"))
    }

    @Test
    @DisplayName("multiple events are processed in sequence")
    fun testMultipleEvents() = runTest {
        val viewModel = createViewModel(emptyFlow())

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        viewModel.processInput(TestEvent.TestEvent2)
        runCurrent()

        assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

        assertThat(vs)
            .hasSize(3)
            .containsExactly(
                TestViewState("[VS] initial"), TestViewState("[VS] 1 "), TestViewState("[VS] 2 ")
            )
    }

    @Test
    @DisplayName("when subsequent events emitted together, ensure correct conflation behavior")
    fun testPreventConflation() = runTest {
        val viewModel = createViewModel(emptyFlow())

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()

        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        // kicking off the subscription (otherwise the processInput will always get conflated)
        runCurrent() // single runCurrent() here could trigger a conflation problem

        viewModel.processInput(TestEvent.TestEvent1)
        viewModel.processInput(TestEvent.TestEvent2)
        runCurrent() // runCurrent() after both events in one shot (increasing chances of conflation)

        // make sure there's no conflation and all expected output received
        assertThat(vs)
            .hasSize(3)
            .containsExactly(
                TestViewState("[VS] initial"), TestViewState("[VS] 1 "), TestViewState("[VS] 2 ")
            )

        assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)
    }

    @Test
    @DisplayName("view state is de-duped when same event processed multiple times, but not effects")
    fun testDuplicateViewState() = runTest {
        val viewModel = createViewModel(emptyFlow())

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        // Send same event twice
        viewModel.processInput(TestEvent.TestEvent1)
        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        // Effects should be emitted for both events
        assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect1)

        // ViewState should only emit once for duplicate state
        assertThat(vs)
            .hasSize(2) // Initial state + one update
            .containsExactly(TestViewState("[VS] initial"), TestViewState("[VS] 1 "))
    }

    @Test
    @DisplayName("concurrent events are processed as soon as they are finished")
    fun testConcurrentEventOrder() = runTest {
        val viewModel = createViewModel(emptyFlow())

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        // Send multiple delayed events rapidly
        viewModel.processInput(TestEvent.TestDelayedEvent(100))
        viewModel.processInput(TestEvent.TestDelayedEvent(50))
        viewModel.processInput(TestEvent.TestDelayedEvent(25))
        advanceTimeBy(101)

        // Verify that effects are processed in order of completion, not submission
        assertThat(es)
            .hasSize(3)
            .containsExactly(
                TestEffect.TestDelayedEffect(25),
                TestEffect.TestDelayedEffect(50),
                TestEffect.TestDelayedEffect(100),
            )
    }

    @Test
    @DisplayName("events sent before any subscribers present, are not lost")
    fun testEventsNotLostWithoutSubscribers() = runTest {
        val viewModel = createViewModel(emptyFlow())

        // Send two events before adding subscribers
        viewModel.processInput(TestEvent.TestEvent1)
        viewModel.processInput(TestEvent.TestEvent2)
        runCurrent()

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()

        // Add subscribers after event was processed
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        assertThat(vs).hasSize(1).containsExactly(TestViewState("[VS] 2 "))
        assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)
    }

    @Test
    @DisplayName("effects sent when subscribers not present, are never lost")
    fun testEffectNotLostWithoutSubscribers() = runTest {
        val viewModel = createViewModel(emptyFlow())

        // Start collecting effects and viewState with first subscriber
        val es1 = mutableListOf<TestEffect>()
        val vs1 = mutableListOf<TestViewState>()
        val jobEs1 = backgroundScope.launch { viewModel.effects.toList(es1) }
        val jobVs1 = backgroundScope.launch { viewModel.viewState.toList(vs1) }
        runCurrent()

        // Process events with first subscriber connected
        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()
        assertThat(es1).hasSize(1)
        assertThat(vs1).hasSize(2)

        // Disconnect first subscriber
        jobEs1.cancel()
        jobVs1.cancel()
        runCurrent()

        // Process events while no subscribers are connected
        viewModel.processInput(TestEvent.TestEvent2)
        runCurrent()

        // Start collecting effects and viewState with second subscriber
        val es2 = mutableListOf<TestEffect>()
        val vs2 = mutableListOf<TestViewState>()
        backgroundScope.launch { viewModel.effects.toList(es2) }
        backgroundScope.launch { viewModel.viewState.toList(vs2) }
        runCurrent()

        assertThat(vs2).last().isEqualTo(TestViewState("[VS] 2 "))

        // Verify that second subscriber received the effects emitted during disconnection
        assertThat(es2).hasSize(1).containsExactly(TestEffect.TestEffect2)
    }

    @Test
    @DisplayName("single event processed, after subscribers disconnect and reconnect are not lost")
    fun testPostSubscriberEvents() = runTest {
        val viewModel = createViewModel(emptyFlow())

        // First subscription
        val vs1 = mutableListOf<TestViewState>()
        val es1 = mutableListOf<TestEffect>()
        val job1 = backgroundScope.launch { viewModel.effects.toList(es1) }
        val job2 = backgroundScope.launch { viewModel.viewState.toList(vs1) }
        runCurrent()

        // Process first event with initial subscribers
        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        // Cancel subscriptions
        job1.cancel()
        job2.cancel()
        runCurrent()

        // Process event while no subscribers
        viewModel.processInput(TestEvent.TestEvent2)
        runCurrent()

        // New subscription
        val vs2 = mutableListOf<TestViewState>()
        val es2 = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es2) }
        backgroundScope.launch { viewModel.viewState.toList(vs2) }
        runCurrent()

        // First subscription should see initial events
        assertThat(vs1)
            .hasSize(2)
            .containsExactly(TestViewState("[VS] initial"), TestViewState("[VS] 1 "))
        assertThat(es1).hasSize(1).containsExactly(TestEffect.TestEffect1)

        // Second subscription should see latest state and replayed effect
        assertThat(vs2).hasSize(1).containsExactly(TestViewState("[VS] 2 "))
        assertThat(es2).hasSize(1).containsExactly(TestEffect.TestEffect2)
    }

    @Test
    @DisplayName("view state is preserved and provided to new subscribers at any time")
    fun testViewStateAlwaysLatest() = runTest {
        val viewModel = createViewModel(emptyFlow())

        // Initial subscription and event
        val vs1 = mutableListOf<TestViewState>()
        val job1 = backgroundScope.launch { viewModel.viewState.toList(vs1) }
        runCurrent()

        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        // Cancel subscription
        job1.cancel()
        runCurrent()

        // Advance time (no effect on state)
        advanceTimeBy(10_000)

        // New subscription should still see the latest view state, but not latest effect
        val ve2 = mutableListOf<TestEffect>()
        val vs2 = mutableListOf<TestViewState>()
        backgroundScope.launch { viewModel.viewState.toList(vs2) }
        backgroundScope.launch { viewModel.effects.toList(ve2) }
        runCurrent()

        assertThat(vs2).hasSize(1).containsExactly(TestViewState("[VS] 1 "))
    }

    @Test
    @DisplayName("effects are only consumed once, even if state is replayed")
    fun testViewEffectsConsumedOnlyOnce() = runTest {
        val viewModel = createViewModel(emptyFlow())

        // First subscriber to effects
        val ve1 = mutableListOf<TestEffect>()
        val job1 = backgroundScope.launch { viewModel.effects.toList(ve1) }
        // Process an event that emits an effect
        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        // First subscriber should receive the effect
        assertThat(ve1).hasSize(1).containsExactly(TestEffect.TestEffect1)

        // Cancel first subscription
        job1.cancel()
        runCurrent()

        // New subscriber to effects
        val ve2 = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(ve2) }
        val vs2 = mutableListOf<TestViewState>()
        backgroundScope.launch { viewModel.viewState.toList(vs2) }
        runCurrent()

        // Second subscriber should not receive the effect again
        assertThat(ve2).isEmpty()
        // ViewState should be replayed to new subscriber
        assertThat(vs2).hasSize(1).containsExactly(TestViewState("[VS] 1 "))
    }

    @Test
    @DisplayName(
        "when error occurs in eventToResultFlow, flow continues processing subsequent events"
    )
    fun testErrorHandling() = runTest {
        val viewModel = createViewModel(emptyFlow())

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        // First send an event that will succeed
        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        // Then send an event that will cause an error
        viewModel.processInput(TestEvent.TestErrorEvent())
        runCurrent()

        // Finally send another valid event
        viewModel.processInput(TestEvent.TestEvent2)
        runCurrent()

        // Verify that effects and view states from valid events were processed
        assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

        assertThat(vs)
            .hasSize(3)
            .containsExactly(
                TestViewState("[VS] initial"), TestViewState("[VS] 1 "), TestViewState("[VS] 2 ")
            )
    }

    @Test
    @DisplayName(
        "when error occurs in resultToViewState, flow continues processing subsequent events"
    )
    fun testErrorInResultToViewState() = runTest {
        val viewModel = createViewModel(emptyFlow())

        val vs = mutableListOf<TestViewState>()
        val es = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effects.toList(es) }
        backgroundScope.launch { viewModel.viewState.toList(vs) }
        runCurrent()

        // First send an event that will succeed
        viewModel.processInput(TestEvent.TestEvent1)
        runCurrent()

        // Then send an event that will cause an error in resultToViewState
        viewModel.processInput(TestEvent.TestErrorInResultToViewStateEvent)
        runCurrent()

        // Finally send another valid event
        viewModel.processInput(TestEvent.TestEvent2)
        runCurrent()

        // Verify that effects from valid events were processed
        assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

        // Verify that view states from valid events were processed, and the error did not stop the flow
        assertThat(vs)
            .hasSize(3)
            .containsExactly(
                TestViewState("[VS] initial"), TestViewState("[VS] 1 "), TestViewState("[VS] 2 ")
            )
    }

    private fun TestScope.createViewModel(injectedFlow: Flow<Int> = emptyFlow()) = TestViewModel(
        backgroundScope,
        testRule.testDispatcher,
        injectedFlow,
    )
}