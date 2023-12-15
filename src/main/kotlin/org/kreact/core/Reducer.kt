package org.kreact.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The function signature of a generic reducer function that takes in an action, state and action
 * dispatcher and provides the resulting mutated or non-mutated state and an optional side effect.
 */
typealias ReducerFunctionType<A, S, E> =
        suspend (A, S, ActionDispatcher<A, S, E>) -> ReducerResult<S, E>

/**
 * An abstract class that processes actions and reduces them to a new state, emitting side effects
 * if necessary.
 *
 * @param A The action to process
 * @param S The current and new state
 * @param E The possible side effect during state mutation
 */
abstract class Reducer<A : Action, S : State, E : SideEffect>(
    scope: CoroutineScope,
    private val actionDispatcher: ActionDispatcher<A, S, E>,
    private val actionFlow: SharedFlow<A>,
    private val stateFlow: MutableStateFlow<S>,
    private val sideEffectFlow: MutableSharedFlow<E>,
    private val dispatchForResultChannel: Channel<DispatchForResultChannelItem<A, S, E>>,
    private val reducerFunction: ReducerFunctionType<A, S, E>,
) {

    private val mutex = Mutex()

    init {
        collectFromActionFlow(scope)
        receiveFromActionChannel(scope)
    }

    /**
     * Listens for actions from the action flow and applies the reducer function to them.
     *
     * @param scope The coroutine scope in which this operation will be performed.
     */
    private fun collectFromActionFlow(scope: CoroutineScope) {
        scope.launch {
            ensureActive()
            actionFlow.collect { action ->
                mutex.withLock {
                    async {
                        emitStateAndSideEffect(
                            reducerFunction.invoke(action, stateFlow.value, actionDispatcher)
                        )
                    }
                }
            }
        }
    }

    /**
     * Listens for actions from the dispatch channel and applies the reducer function to them.
     *
     * @param scope The coroutine scope in which this operation will be performed.
     */
    private fun receiveFromActionChannel(scope: CoroutineScope) {
        scope.launch {
            ensureActive()
            while (true) {
                val channelItem = dispatchForResultChannel.receive()
                mutex.withLock {
                    channelItem.action?.let { action ->
                        val reducerResult = reducerFunction.invoke(
                            action, stateFlow.value, actionDispatcher
                        )
                        emitStateAndSideEffect(reducerResult)
                        dispatchForResultChannel.send(
                            DispatchForResultChannelItem(action, reducerResult)
                        )
                    }
                }
            }
        }
    }

    /**
     * Emits the new state and any side effects that resulted from the reducer function.
     *
     * @param reducerResult The result from the reducer function.
     */
    private suspend fun emitStateAndSideEffect(reducerResult: ReducerResult<S, E>) {
        reducerResult.state?.let {
            stateFlow.emit(it)
        }
        reducerResult.sideEffect?.let {
            sideEffectFlow.emit(it)
        }
    }
}