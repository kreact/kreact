package org.kreact.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An implementation of the [StateProvider]
 *
 * @param S The state to provide
 * @param E The possible side effects during state mutation
 */
internal class StateProviderImpl<S : State, E : SideEffect>(
    override val stateFlow: MutableStateFlow<S>,
    override val sideEffectFlow: MutableSharedFlow<E>,
) : StateProvider<S, E>()