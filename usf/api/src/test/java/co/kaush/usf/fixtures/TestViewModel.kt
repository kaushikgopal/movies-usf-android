package co.kaush.usf.fixtures

import co.kaush.usf.TestLogger
import co.kaush.usf.UsfViewModelImpl
import co.kaush.usf.fixtures.TestEffect.TestDelayedEffect
import co.kaush.usf.fixtures.TestEvent.TestDelayedEvent
import co.kaush.usf.fixtures.TestEvent.TestErrorEvent
import co.kaush.usf.fixtures.TestEvent.TestErrorInResultToEffectsEvent
import co.kaush.usf.fixtures.TestEvent.TestErrorInResultToViewStateEvent
import co.kaush.usf.fixtures.TestEvent.TestEvent1
import co.kaush.usf.fixtures.TestEvent.TestEvent2
import co.kaush.usf.fixtures.TestEvent.TestInitFlowEvent
import co.kaush.usf.fixtures.TestResult.TestDelayedResult
import co.kaush.usf.fixtures.TestResult.TestErrorInResultToEffectsResult
import co.kaush.usf.fixtures.TestResult.TestErrorInResultToViewStateResult
import co.kaush.usf.fixtures.TestResult.TestErrorResult
import co.kaush.usf.fixtures.TestResult.TestInitFlowResult
import co.kaush.usf.fixtures.TestResult.TestNullableEffectResult
import co.kaush.usf.fixtures.TestResult.TestResult1
import co.kaush.usf.fixtures.TestResult.TestResult2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TestViewModel(
    coroutineScope: CoroutineScope,
    processingDispatcher: CoroutineDispatcher,
    initFlow: Flow<Int> = emptyFlow(),
) :
    UsfViewModelImpl<TestEvent, TestResult, TestViewState, TestEffect, Unit>(
        initialState = TestViewState("[VS] initial"),
        coroutineScope = coroutineScope,
        processingDispatcher = processingDispatcher,
        logger = TestLogger,
    ) {
    init {
        initFlow.onEach { processInput(TestInitFlowEvent(it)) }.launchIn(coroutineScope)
    }

    override suspend fun eventToResultFlow(event: TestEvent): Flow<TestResult> {
        return when (event) {
            TestEvent1 -> flowOf(TestResult1)
            TestEvent2 -> flowOf(TestResult2)
            is TestInitFlowEvent -> flowOf(TestInitFlowResult(event.value))
            is TestErrorEvent -> throw event.error
            is TestDelayedEvent ->
                flowOf(TestDelayedResult(event.delayMs)).onEach { delay(event.delayMs) }

            TestEvent.TestNullableEffectEvent -> flowOf(TestNullableEffectResult)
            is TestErrorInResultToViewStateEvent -> flowOf(TestErrorInResultToViewStateResult)
            is TestErrorInResultToEffectsEvent -> flowOf(TestErrorInResultToEffectsResult)
        }
    }

    override fun resultToViewState(
        currentViewState: TestViewState,
        result: TestResult
    ): TestViewState {
        return when (result) {
            TestResult1 -> currentViewState.copy(text = "[VS] 1 ")
            TestResult2 -> currentViewState.copy(text = "[VS] 2 ")
            is TestInitFlowResult -> currentViewState.copy(number = result.value)
            is TestErrorResult -> currentViewState
            is TestDelayedResult -> currentViewState.copy(text = "[VS] delayed ${result.delayMs}")
            TestNullableEffectResult -> currentViewState.copy(text = "[VS] nullable effect")
            TestErrorInResultToViewStateResult -> throw Exception("Error in resultToViewState")
            TestErrorInResultToEffectsResult -> currentViewState
        }
    }

    override fun resultToEffects(result: TestResult): Flow<TestEffect> {
        return when (result) {
            TestResult1 -> {
                flowOf(TestEffect.TestEffect1)
            }

            TestResult2 -> {
                flowOf(TestEffect.TestEffect2)
            }

            is TestInitFlowResult -> {
                flowOf(TestEffect.TestInitFlowEffect(result.value))
            }

            is TestDelayedResult -> {
                flowOf(TestDelayedEffect(result.delayMs))
            }

            TestErrorInResultToEffectsResult -> {
                throw Exception("Error in resultToEffects")
            }

            TestNullableEffectResult -> {
                emptyFlow()
            }

            TestErrorInResultToViewStateResult -> {
                emptyFlow()
            }

            is TestErrorResult -> {
                emptyFlow()
            }
        }
    }
}

sealed class TestEvent {
    data object TestEvent1 : TestEvent()

    data object TestEvent2 : TestEvent()

    data class TestInitFlowEvent(val value: Int) : TestEvent()

    data class TestErrorEvent(val error: Throwable = RuntimeException("Test error")) : TestEvent()

    data class TestDelayedEvent(val delayMs: Long) : TestEvent()

    data object TestNullableEffectEvent : TestEvent()

    data object TestErrorInResultToViewStateEvent : TestEvent()

    data object TestErrorInResultToEffectsEvent : TestEvent()
}

sealed class TestResult {
    data object TestResult1 : TestResult()

    data object TestResult2 : TestResult()

    data class TestInitFlowResult(val value: Int) : TestResult()

    data class TestErrorResult(val error: Throwable) : TestResult()

    data class TestDelayedResult(val delayMs: Long) : TestResult()

    data object TestNullableEffectResult : TestResult()

    data object TestErrorInResultToViewStateResult : TestResult()

    data object TestErrorInResultToEffectsResult : TestResult()
}

sealed class TestEffect {
    data object TestEffect1 : TestEffect()

    data object TestEffect2 : TestEffect()

    data class TestInitFlowEffect(val value: Int) : TestEffect()

    data class TestDelayedEffect(val delayMs: Long) : TestEffect()
}

data class TestViewState(
    val text: String,
    val number: Int = -1,
)
