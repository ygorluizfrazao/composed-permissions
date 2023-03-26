package br.com.frazo.easy_permissions.base.strategy

/**
 * Enum that represents the possible states of a [PermissionAskingStrategy] instance.
 */
enum class PermissionFlowStateEnum {
    /**
     * The strategy is in its initial state.
     */
    NOT_STARTED,

    /**
     * Started processing.
     */
    STARTED,

    /**
     * The permission request was denied.
     */
    DENIED_BY_SYSTEM,
    /**
     * Waiting for app handled prompt.
     */
    APP_PROMPT,

    /**
     * Permission(s) granted.
     */
    TERMINAL_GRANTED,

    /**
     * Permissions(s) denied.
     */
    TERMINAL_DENIED
}