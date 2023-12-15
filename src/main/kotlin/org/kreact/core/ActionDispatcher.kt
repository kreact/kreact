package org.kreact.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * An abstract class that manages action dispatching.
 *
 * @param A The type of action dispatched
 * @param S The type of state
 * @param E The type of side effect that can occur during the state mutation
 */
abstract class ActionDispatcher<A : Action, S : State, E : SideEffect>(
    private val actionFlow: MutableSharedFlow<A>,
    private val actionReceiveChannel: Channel<DispatchForResultChannelItem<A, S, E>>,
) {

    /**
     * Dispatches an action to the flow for processing by the reducer.
     *
     * @param action The action to dispatch.
     */
    suspend fun dispatch(action: A) {
        actionFlow.emit(action)
    }

    /**
     * Dispatches an action to the channel for processing and waits for the result from the reducer.
     *
     * @param action The action to dispatch with suspension.
     */
    suspend fun dispatchAndAwait(action: A) {
        actionReceiveChannel.send(DispatchForResultChannelItem(action))
        actionReceiveChannel.receive()
    }
}