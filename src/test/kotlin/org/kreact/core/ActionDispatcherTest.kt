package org.kreact.core

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActionDispatcherTest {

    private lateinit var actionFlow: MutableSharedFlow<Action>
    private lateinit var actionReceiveChannel: Channel<DispatchForResultChannelItem<Action, State, SideEffect>>
    private lateinit var actionDispatcher: ActionDispatcher<Action, State, SideEffect>
    private val testScope = TestScope()


    @BeforeEach
    fun setUp() {
        actionFlow = mockk(relaxed = true)
        actionReceiveChannel = mockk(relaxed = true)
        actionDispatcher = object : ActionDispatcher<Action, State, SideEffect>(
            actionFlow, actionReceiveChannel
        ) {}
        Dispatchers.setMain(StandardTestDispatcher(testScope.testScheduler))
    }

    @AfterEach
    fun clean() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dispatch should emit action to actionFlow`() = runTest {
        val action = mockk<Action>()

        actionDispatcher.dispatch(action)
        advanceUntilIdle()

        coVerify { actionFlow.emit(action) }
    }

    @Test
    fun `dispatchAndAwait should send action to actionReceiveChannel and receive a result`() = runTest {
        val action = mockk<Action>()

        coEvery { actionReceiveChannel.send(any()) } coAnswers { nothing }
        coEvery { actionReceiveChannel.receive() } returns DispatchForResultChannelItem(action)

        actionDispatcher.dispatchAndAwait(action)
        advanceUntilIdle()

        coVerify { actionReceiveChannel.send(any()) }
        coVerify { actionReceiveChannel.receive() }
    }
}