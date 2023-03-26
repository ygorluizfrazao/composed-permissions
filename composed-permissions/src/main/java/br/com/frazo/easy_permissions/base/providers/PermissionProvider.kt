package br.com.frazo.easy_permissions.base.providers

/**
 * Contract for Permission Providers. Assumes platform independence.
 *
 * @param P generic return type.
 */
interface PermissionProvider<out P> {

    /**
     * Abstraction which have to be implemented to return a meaningful permission given a platform context.
     *
     * @return [P]
     */
    fun provide(): P

}