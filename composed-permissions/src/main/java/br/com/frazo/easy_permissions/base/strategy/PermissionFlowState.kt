package br.com.frazo.easy_permissions.base.strategy

/**
 * Data class that associates an [state]([PermissionFlowStateEnum]) with some payload
 * data.
 *
 * @param [D] Generic type of the payload.
 *
 * @param state [PermissionFlowStateEnum]
 * @param data [D]
 */
data class PermissionFlowState<D>(
    val state: PermissionFlowStateEnum,
    val data: D
)