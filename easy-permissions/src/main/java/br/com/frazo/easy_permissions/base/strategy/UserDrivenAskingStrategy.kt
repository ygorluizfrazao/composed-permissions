package br.com.frazo.easy_permissions.base.strategy

/**
 * A Contract that inherits from [PermissionAskingStrategy], intended to be able to change its state
 * based on user manual interactions with the hosting application.
 *
 * @param [D] Generic type passed to [PermissionAskingStrategy]
 */
interface UserDrivenAskingStrategy<D> :
    PermissionAskingStrategy<D> {

    /**
     * Exposed function that should be called when the user manually denies a permission inside de
     * hosting Application scope.
     *
     * It should be processed by the [UserDrivenAskingStrategy] resulting in a state change.
     */
    fun onUserManuallyDenied()

    /**
     * Exposed function that should be called when the user is prompted to grant a permission inside de
     * hosting Application scope.
     *
     * It should be processed by the [UserDrivenAskingStrategy] resulting in a state change.
     */
    fun onRequestedUserManualGrant()

}