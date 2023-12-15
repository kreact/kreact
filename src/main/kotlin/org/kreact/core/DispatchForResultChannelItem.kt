package org.kreact.core

/**
 * A data class representing a dispatched action item for processing and the resulting state
 * mutation. This is provided only if the dispatched action was dispatched for a result and thus
 * the coroutine will suspend until the result is received. Used only in combination with
 * [ActionDispatcher.dispatchAndAwait].
 *
 * @param A The type of action dispatched to the reducer
 * @param S The type of resulting state after reduction
 * @param E The type of side effect after reduction
 */
data class DispatchForResultChannelItem<A : Action, S : State, E : SideEffect>(
    val action: A? = null,
    val reducerResult: ReducerResult<S, E>? = null,
)