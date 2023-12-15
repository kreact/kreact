package org.kreact.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A factory object for creating an [ActionDispatcher] and [StateProvider].
 */
object StateProviderFactory {

    /**
     * Creates an [ActionDispatcher] and a [StateProvider], properly wired together,
     * ready to be used.
     *
     * @param scope The coroutine scope in which reducer operations will be performed.
     * @param initialState The initial state.
     * @param reducerFunctions The functions that will be used to reduce the state.
     * @return A pair of [ActionDispatcher] and [StateProvider].
     */
    fun <A : Action, S : State, E : SideEffect> create(
        scope: CoroutineScope,
        initialState: S,
        vararg reducerFunctions: ReducerFunctionType<A, S, E>,
    ): Pair<ActionDispatcher<A, S, E>, StateProvider<S, E>> {

        val actionFlow = MutableSharedFlow<A>()
        val stateFlow = MutableStateFlow(initialState)
        val sideEffectFlow = MutableSharedFlow<E>()
        val reducerResultChannel = Channel<DispatchForResultChannelItem<A, S, E>>()

        val actionDispatcher = object : ActionDispatcher<A, S, E>(
            actionFlow, reducerResultChannel
        ) {}

        reducerFunctions.forEach {
            object : Reducer<A, S, E>(
                scope,
                actionDispatcher,
                actionFlow,
                stateFlow,
                sideEffectFlow,
                reducerResultChannel,
                it
            ) {}
        }

        val stateManager = StateProviderImpl(stateFlow, sideEffectFlow)

        return actionDispatcher to stateManager
    }
}