package org.kreact.core

/**
 * A sealed class representing the result of the reducer function.
 * It either returns a new state and possible side effect (Mutation), or no new state with a
 * possible side effect (NoMutation).
 *
 * @param S The resulting state
 * @param E The possible side effect during state mutation
 */
sealed class ReducerResult<S : State, E : SideEffect>(open val state: S?, open val sideEffect: E?) {

    /**
     * This class indicates that the state mutation occurred successfully.
     *
     * @param state the new state following successful mutation
     * @param sideEffect the side effect following successful mutation
     */
    data class Mutation<S : State, E : SideEffect>(
        override val state: S,
        override val sideEffect: E? = null,
    ) : ReducerResult<S, E>(state, sideEffect)

    /**
     * This class indicates that the state mutation did not occur. As such the state is not
     * provided.
     *
     * This result should be treated as neutral. Meaning something occurred in the reducer and the
     * state did not have to change as a result of it.
     *
     * @param sideEffect the side effect following no mutation
     */
    data class NoMutation<S : State, E : SideEffect>(
        override val sideEffect: E? = null,
    ) : ReducerResult<S, E>(null, sideEffect)
}