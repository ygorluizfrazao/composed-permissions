package br.com.frazo.easy_permissions.base.providers.android

import br.com.frazo.easy_permissions.base.providers.PermissionProvider
import br.com.frazo.easy_permissions.util.capitalizeWords

/**
 * Contract for Android Permission Providers. Overrides [PermissionProvider] using [List] of [String]
 * as the generic return type as that's the way Android defines it's permissions.
 *
 * @property name The name of the permissions.
 * Ex.: The permission for audio recording in Android is named "android.permission.RECORD_AUDIO", [name]
 * with return "Record Audio\n" by default.
 */
interface AndroidPermissionProvider : PermissionProvider<List<String>> {

    /**
     * The name of the permissions.
     *
     * By default it will return a human understandable name.
     * Ex.: The permission for audio recording in Android is named "android.permission.RECORD_AUDIO", [name]
     * with return "Record Audio\n" by default.
     */
    val name: String
        get() {
            return provide().map { it.toHumanLanguage() + "\n" }.reduce { acc, s ->
                acc + s
            }
        }


    companion object {

        /**
         * Convenience extension designed to be used with Android permissions.
         * Returns a human understandable name.
         *
         * @receiver [String]
         * @sample [toHumanLanguage]
         */
        fun String.toHumanLanguage(): String {
            return this.split(".").last().replace("_", " ").capitalizeWords()
        }

        /**
         * Convenience function to create an anonymous class implementation of [AndroidPermissionProvider]
         */
        fun of(vararg permissions: String): AndroidPermissionProvider {
            return of(permissions.toList())
        }

        /**
         * Convenience function to create an anonymous class implementation of [AndroidPermissionProvider]
         */
        fun of(permissions: List<String>): AndroidPermissionProvider {
            return object : AndroidPermissionProvider {
                val permissionsList = permissions.distinct()
                override fun provide(): List<String> {
                    return permissionsList
                }

            }
        }
    }
}