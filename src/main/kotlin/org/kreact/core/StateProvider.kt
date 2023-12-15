package org.kreact.core

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * An abstract class that provides a flow of states and side effects.
 *
 * @param S The state to provide
 * @param E The possible side effects during state mutation
 *
 * @property stateFlow is a hot stream of [State] values emitted once and only once to its
 * collectors. It has a default initial state value.
 * @property sideEffectFlow is a hot stream of [SideEffect] values emitted once or more times to its
 * collectors, dependent on the replay value during collection, and has no initial value.
 */
abstract class StateProvider<S : State, E : SideEffect> {
    abstract val stateFlow: StateFlow<S>
    abstract val sideEffectFlow: SharedFlow<E>
}