package br.com.frazo.easy_permissions.base.requesters.android

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import br.com.frazo.easy_permissions.base.findActivityResultCaller
import br.com.frazo.easy_permissions.base.providers.android.AndroidPermissionProvider
import br.com.frazo.easy_permissions.base.requesters.PermissionRequester

/**
 * Android specific [PermissionRequester] implementation. it cannot be instantiated directly, instead,
 * you should use the extension function [register] inside you [ActivityResultCaller] initialization.
 *
 * When using compose you should use [rememberAndroidPermissionRequester], [rememberAndroidPermissionRequester],
 * [rememberAndroidPermissionRequester] or [rememberAndroidPermissionRequester] to ensure proper remembering and
 * disposal of the created [ActivityResultLauncher].
 *
 * @property [permissionProvider] an [AndroidPermissionProvider] that will be used to determine how the permission
 * is structured in the platform.
 *
 */
class AndroidPermissionRequester private constructor(
    private val activityResultCaller: ActivityResultCaller,
    private val context: Context,
    override val permissionProvider: AndroidPermissionProvider
) : PermissionRequester<List<String>, String> {

    /**
     * Callback for a permission resolving result.
     */
    private var onPermissionResult: ((Map<String, Boolean>) -> Unit)? = null

    /**
     * [ActivityResultLauncher] created internally when registering the [androidx.activity.result.contract.ActivityResultContract]
     */
    private var activityResultLauncher: ActivityResultLauncher<*>? = null


    /**
     * Registers the [activityResultCaller] with the proper [androidx.activity.result.contract.ActivityResultContract] according
     * to the supplied [permissionProvider].
     */
    private fun registerLauncher() {
        activityResultLauncher?.unregister()

        activityResultLauncher = if (permissionProvider.isSinglePermission()) {
            activityResultCaller.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                onPermissionResult(mapOf(Pair(permissionProvider.provide().first(), it)))
            }
        } else {
            activityResultCaller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                onPermissionResult(it)
            }
        }
    }

    /**
     * Call Android system to ask for the given permissions according to [permissionProvider].
     *
     * @param resultCallback The Callback that will receive the result of the call.
     */
    override fun ask(resultCallback: (Map<String, Boolean>) -> Unit) {

        onPermissionResult = resultCallback
        if (permissionProvider.isSinglePermission()) {
            @Suppress("UNCHECKED_CAST")
            (activityResultLauncher as ActivityResultLauncher<String>).launch(
                permissionProvider.provide().first()
            )
        } else {
            @Suppress("UNCHECKED_CAST")
            (activityResultLauncher as ActivityResultLauncher<Array<String>>).launch(
                permissionProvider.provide().toTypedArray()
            )
        }
    }

    /**
     * Asks Android about the current status of the given permissions provided by [permissionProvider].
     *
     * @return [Map]<[String],[Boolean]>
     *
     * @sample [permissionsStatus]
     */
    override fun permissionsStatus(): Map<String, Boolean> {
        return permissionProvider.provide().associateWith {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Convenience method that receives Android's response to [ask], to [onPermissionResult] callback supplied in [ask].
     *
     */
    private fun onPermissionResult(resultMap: Map<String, Boolean>) {
        onPermissionResult?.invoke(resultMap)
    }

    companion object {

        /**
         * Creates an [AndroidPermissionRequester] and registers a [ActivityResultContract]([androidx.activity.result.contract.ActivityResultContract])
         * in accord with the supplied [permissionProvider].
         *
         * It should not be used with compose as it will result in memory leak.
         *
         * @receiver [ActivityResultCaller]
         *
         * @param permissionProvider [AndroidPermissionProvider]
         * @param context [Context] context of this [ActivityResultCaller].
         *
         * @return [AndroidPermissionRequester]
         */
        fun ActivityResultCaller.register(
            permissionProvider: AndroidPermissionProvider,
            context: Context
        ): AndroidPermissionRequester {
            return AndroidPermissionRequester(
                this,
                context,
                permissionProvider
            ).apply { registerLauncher() }
        }

        /**
         * Creates and [remember] a compose lifecycle aware [AndroidPermissionRequester].
         *
         * @receiver [ActivityResultCaller]
         *
         * @param context [Context] context of this [ActivityResultCaller].
         * @param permissionProvider [AndroidPermissionProvider]
         *
         * @return [AndroidPermissionRequester]
         */
        @Composable
        fun ActivityResultCaller.rememberAndroidPermissionRequester(
            context: Context = LocalContext.current,
            permissionProvider: AndroidPermissionProvider
        ): AndroidPermissionRequester {

            val permissionState = rememberUpdatedState(newValue = permissionProvider)
            val contextState = rememberUpdatedState(newValue = context)
            val activityResultCallerState = rememberUpdatedState(newValue = this)

            val androidPermissionRequester = remember {
                AndroidPermissionRequester(
                    activityResultCallerState.value,
                    contextState.value,
                    permissionState.value
                )
            }

            androidPermissionRequester.activityResultLauncher =
                if (permissionProvider.isSinglePermission()) {
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = {
                            androidPermissionRequester.onPermissionResult(
                                mapOf(
                                    Pair(
                                        permissionProvider.provide().first(), it
                                    )
                                )
                            )
                        })
                } else {
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions(),
                        onResult = {
                            androidPermissionRequester.onPermissionResult(it)
                        })
                }

            return androidPermissionRequester
        }

        /**
         * Creates and [remember] a compose lifecycle aware [AndroidPermissionRequester].
         *
         * @param context [Context] context used to infer the [activityResultCaller] param.
         * @param permissionProvider [AndroidPermissionProvider]
         *
         * @return [AndroidPermissionRequester]
         */
        @Composable
        fun rememberAndroidPermissionRequester(
            context: Context = LocalContext.current,
            permissionProvider: AndroidPermissionProvider
        ): AndroidPermissionRequester {

            return context.findActivityResultCaller().rememberAndroidPermissionRequester(
                permissionProvider = permissionProvider,
                context = context
            )
        }

        /**
         * Creates and [remember] a compose lifecycle aware [AndroidPermissionRequester].
         *
         * @param context [Context] context used to infer the [activityResultCaller] param.
         * @param permissions used to create the [permissionProvider] by use of the convenience function
         * [AndroidPermissionProvider.of]
         *
         * @return [AndroidPermissionRequester]
         */
        @Composable
        fun rememberAndroidPermissionRequester(
            context: Context = LocalContext.current,
            vararg permissions: String
        ): AndroidPermissionRequester {
            return rememberAndroidPermissionRequester(context, permissions = permissions.toList())
        }

        /**
         * Creates and [remember] a compose lifecycle aware [AndroidPermissionRequester].
         *
         * @param context [Context] context used to infer the [activityResultCaller] param.
         * @param permissions used to create the [permissionProvider] by use of the convenience function
         * [AndroidPermissionProvider.of]
         *
         * @return [AndroidPermissionRequester]
         */
        @Composable
        fun rememberAndroidPermissionRequester(
            context: Context = LocalContext.current,
            permissions: List<String>
        ): AndroidPermissionRequester {
            return context.findActivityResultCaller().rememberAndroidPermissionRequester(
                permissionProvider = AndroidPermissionProvider.of(permissions.toList()),
                context = context
            )
        }
    }
}

/**
 * Convenience extension function to determine if an [AndroidPermissionProvider] deals with a single permission
 * or with multiple permissions.
 *
 * @receiver [AndroidPermissionProvider]
 *
 * @return [Boolean], true is single permission, false otherwise.
 */
private fun AndroidPermissionProvider.isSinglePermission(): Boolean {
    if (this.provide().size > 1)
        return false
    return true
}
