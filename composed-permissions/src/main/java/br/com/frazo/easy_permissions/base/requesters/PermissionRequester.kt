package br.com.frazo.easy_permissions.base.requesters

import br.com.frazo.easy_permissions.base.providers.PermissionProvider

/**
 * Contract to define a Permission Requester object. it assumes platform independence.
 *
 * @param D Generic type of [PermissionProvider]
 * @param R Generic return type used in [ask] and [permissionsStatus], usually, [D] is a collection
 * of [R]'s.
 * @property [permissionProvider] a [PermissionProvider] that will be used to determine how the permission
 * is structured in the platform.
 *
 */
interface PermissionRequester<D,R> {

    /**
     * A [PermissionProvider] that will be used to determine how the permission
     * is structured in the platform.
     */
    val permissionProvider: PermissionProvider<D>

    /**
     * Method which asynchronously asks the platform for a permission as specified in [permissionProvider].
     *
     * @param resultCallback will be called once the response is resolved.
     */
    fun ask(resultCallback: (Map<R, Boolean>) -> Unit)

    /**
     * Method which synchronously asks the platform for a permission as specified in [permissionProvider]
     * and returns a [Map] with the permissions status, (granted or not granted).
     * @return [Map]<[R],[Boolean]>
     */
    fun permissionsStatus(): Map<R, Boolean>

}