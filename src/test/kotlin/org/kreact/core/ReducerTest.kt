package org.kreact.core

import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReducerTest {

    private sealed class TestAction : Action {
        data object NewStateAction : TestAction()
        data object SideEffectAction : TestAction()
    }

    private data class TestState(val changed: Boolean = false) : State
    private object TestSideEffect : SideEffect

    private object TestReducerFunction :
        ReducerFunctionType<TestAction, TestState, TestSideEffect> {
        override suspend fun invoke(
            action: TestAction,
            state: TestState,
            actionDispatcher: ActionDispatcher<TestAction, TestState, TestSideEffect>
        ): ReducerResult<TestState, TestSideEffect> {
            return when (action) {
                is TestAction.NewStateAction -> ReducerResult.Mutation(
                    state.copy(changed = true)
                )

                is TestAction.SideEffectAction -> ReducerResult.NoMutation(TestSideEffect)
            }
        }
    }

    private lateinit var actionFlow: MutableSharedFlow<TestAction>
    private lateinit var stateFlow: MutableStateFlow<TestState>
    private lateinit var sideEffectFlow: MutableSharedFlow<TestSideEffect>
    private lateinit var dispatchForResultChannel:
            Channel<DispatchForResultChannelItem<TestAction, TestState, TestSideEffect>>
    private lateinit var actionDispatcher: ActionDispatcher<TestAction, TestState, TestSideEffect>

    private val testScope = TestScope()

    @BeforeEach
    fun setUp() {
        actionFlow = MutableSharedFlow()
        stateFlow = spyk(MutableStateFlow(TestState()))
        sideEffectFlow = spyk(MutableSharedFlow())
        dispatchForResultChannel = Channel()

        actionDispatcher = object : ActionDispatcher<TestAction, TestState, TestSideEffect>(
            actionFlow, dispatchForResultChannel
        ) {}

        object : Reducer<TestAction, TestState, TestSideEffect>(
            testScope,
            actionDispatcher,
            actionFlow,
            stateFlow,
            sideEffectFlow,
            dispatchForResultChannel,
            TestReducerFunction
        ) {}
        Dispatchers.setMain(StandardTestDispatcher(testScope.testScheduler))
    }

    @AfterEach
    fun clean() {
        Dispatchers.resetMain()
    }

    @Test
    fun `action flow emits state`() = runTest {
        val action = TestAction.NewStateAction
        val newState = TestState(changed = true)

        actionFlow.emit(action)
        advanceUntilIdle()

        coVerify { stateFlow.emit(newState) }
    }

    @Test
    fun `action flow emits side effect`() = runTest {
        val action = TestAction.SideEffectAction
        val sideEffect = TestSideEffect

        actionFlow.emit(action)
        advanceUntilIdle()

        coVerify { sideEffectFlow.emit(sideEffect) }
    }
}