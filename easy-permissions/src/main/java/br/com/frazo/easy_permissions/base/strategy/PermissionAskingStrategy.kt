package br.com.frazo.easy_permissions.base.strategy

import kotlinx.coroutines.flow.Flow

/**
 * Contract that defines how to handle the process of permission asking. It supposes platform independence.
 * It was designed to work as a [Finite State Machine](https://en.wikipedia.org/wiki/Finite-state_machine).
 *
 * @param [D] Generic type of the data payload in [PermissionFlowState]
 *
 */
interface PermissionAskingStrategy<D> {
    /**
     * Used to resolve the next state of the machine. its assumed that all information needed to infer
     * the next state is handled by the inheritor class.
     */
    fun resolveState()

    /**
     * Exposes the state of the machine with as a [Flow] of [PermissionFlowState]<[D]>
     *
     * @return [PermissionFlowState]<[D]>
     */
    fun flowState(): Flow<PermissionFlowState<D>>
}

