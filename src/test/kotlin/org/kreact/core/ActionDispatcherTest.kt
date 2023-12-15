package org.kreact.core

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    }

    @Test
    fun `dispatch should emit action to actionFlow`() = testScope.runTest {
        val action = mockk<Action>()
        actionDispatcher.dispatch(action)

        coVerify { actionFlow.emit(action) }
    }

    @Test
    fun `dispatchAndAwait should send action to actionReceiveChannel and receive a result`() = testScope.runTest {
        val action = mockk<Action>()

        coEvery { actionReceiveChannel.send(any()) } coAnswers { nothing }
        coEvery { actionReceiveChannel.receive() } returns DispatchForResultChannelItem(action)

        actionDispatcher.dispatchAndAwait(action)

        coVerify { actionReceiveChannel.send(any()) }
        coVerify { actionReceiveChannel.receive() }
    }
}