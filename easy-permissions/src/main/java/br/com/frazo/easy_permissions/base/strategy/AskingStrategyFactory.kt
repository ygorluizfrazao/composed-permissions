package br.com.frazo.easy_permissions.base.strategy

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import br.com.frazo.easy_permissions.base.requesters.PermissionRequester
import br.com.frazo.easy_permissions.base.requesters.android.AndroidPermissionRequester

/**
 * Possible Permission Asking Strategies.
 *
 * Used with [rememberUserDrivenAskingStrategy] to create the appropriate [UserDrivenAskingStrategy]
 * instance for the given need.
 */
enum class AskingStrategy {
    /**
     * This strategy consists in keep asking for permission while exists denied/not granted permissions.
     *
     * The internal state machine can assume the following states
     * [PermissionFlowStateEnum.NOT_STARTED],
     * [PermissionFlowStateEnum.STARTED],
     * [PermissionFlowStateEnum.DENIED_BY_SYSTEM],
     * [PermissionFlowStateEnum.APP_PROMPT] and
     * [PermissionFlowStateEnum.TERMINAL_GRANTED].
     *
     * @see KeepAskingStrategy
     */
    KEEP_ASKING,

    /**
     * This strategy consists in only asking for permission to the system, no additional custom
     * prompt is expected to be showed to the the user. In summary, if exists denied/not granted
     * it will ask the system at least twice. After that, if still exists denied/not granted
     * permissions, it will assume the [PermissionFlowStateEnum.TERMINAL_DENIED] state.
     *
     *
     * The internal state machine can assume the following states
     * [PermissionFlowStateEnum.NOT_STARTED],
     * [PermissionFlowStateEnum.STARTED],
     * [PermissionFlowStateEnum.TERMINAL_GRANTED].
     * [PermissionFlowStateEnum.TERMINAL_DENIED]
     *
     * @see OnlyAskSystem
     */
    ONLY_ASK_SYSTEM,

    /**
     * This strategy consists in keep asking for permission while exists denied/not granted
     * permissions and the user has not denied the permissions manually inside the instance
     * scope.
     *
     *
     * The internal state machine can assume the following states
     * [PermissionFlowStateEnum.NOT_STARTED],
     * [PermissionFlowStateEnum.STARTED],
     * [PermissionFlowStateEnum.DENIED_BY_SYSTEM]
     * [PermissionFlowStateEnum.APP_PROMPT]
     * [PermissionFlowStateEnum.TERMINAL_GRANTED].
     * [PermissionFlowStateEnum.TERMINAL_DENIED]
     *
     * @see StopOnUserDenialAskingStrategy
     */
    STOP_ASKING_ON_USER_DENIAL,

    /**
     * It's your time to shine dear friend. Use this [AskingStrategy] when you supply your own [UserDrivenAskingStrategy]
     * implementation.
     */
    CUSTOM
}

/**
 * Convenience function that works like a factory to create the proper [UserDrivenAskingStrategy]
 * based on the supplied [AskingStrategy].
 *
 * You can pass your custom factory in [customFactory] parameter.
 *
 * Note: This method does not create a Compose aware instance
 *
 * @param type [AskingStrategy] which defines the [UserDrivenAskingStrategy] that will be created.
 * @param permissionRequester [PermissionRequester] that will be used in the [UserDrivenAskingStrategy]
 * creation
 * @param canStart Lambda that will be used in the [UserDrivenAskingStrategy] creation, as defined in contract.
 * @param customFactory Optional lambda that creates custom made instances of [UserDrivenAskingStrategy]
 *
 * @return [UserDrivenAskingStrategy]
 *
 * @throws [UndefinedAskingStrategy] If it cannot create an instance of the supplied [AskingStrategy]
 *
 *
 * @see [AskingStrategy],[UserDrivenAskingStrategy],[PermissionRequester]
 */
fun createUserDrivenAskingStrategy(
    type: AskingStrategy,
    permissionRequester: PermissionRequester<List<String>, String>,
    canStart: () -> Boolean = { true },
    customFactory: ((
        type: AskingStrategy,
        permissionRequester: PermissionRequester<List<String>, String>,
        canStart: () -> Boolean
    ) -> UserDrivenAskingStrategy<Map<String, Boolean>>)? = null
): UserDrivenAskingStrategy<Map<String, Boolean>> {


    return when (type) {
        AskingStrategy.STOP_ASKING_ON_USER_DENIAL -> StopOnUserDenialAskingStrategy(
            permissionRequester,
            canStart
        )
        AskingStrategy.KEEP_ASKING -> KeepAskingStrategy(permissionRequester, canStart)
        AskingStrategy.ONLY_ASK_SYSTEM -> OnlyAskSystem(permissionRequester, canStart)
        else -> {
            customFactory?.invoke(type, permissionRequester, canStart)
                ?: throw UndefinedAskingStrategy(type)
        }
    }
}

/**
 * Convenience function that works like a factory to create and [remember] the proper [UserDrivenAskingStrategy]
 * based on the supplied [AskingStrategy].
 *
 * You can pass your custom factory in [customFactory] parameter.
 *
 * @param type [AskingStrategy] which defines the [UserDrivenAskingStrategy] that will be created.
 * @param permissionRequester [PermissionRequester] that will be used in the [UserDrivenAskingStrategy]
 * creation
 * @param canStart Lambda that will be used in the [UserDrivenAskingStrategy] creation, as defined in contract.
 * @param customFactory Optional lambda that creates custom made instances of [UserDrivenAskingStrategy]
 *
 * @return [UserDrivenAskingStrategy]
 *
 * @throws [UndefinedAskingStrategy] If it cannot create an instance of the supplied [AskingStrategy]
 *
 * @see [AskingStrategy],[UserDrivenAskingStrategy],[PermissionRequester]
 */
@Composable
fun rememberUserDrivenAskingStrategy(
    type: AskingStrategy,
    permissionRequester: PermissionRequester<List<String>, String>,
    canStart: () -> Boolean,
    customFactory: ((
        type: AskingStrategy,
        permissionRequester: PermissionRequester<List<String>, String>,
        canStart: () -> Boolean
    ) -> UserDrivenAskingStrategy<Map<String, Boolean>>)? = null
): UserDrivenAskingStrategy<Map<String, Boolean>> {

    return when (type) {
        AskingStrategy.STOP_ASKING_ON_USER_DENIAL -> {
            StopOnUserDenialAskingStrategy.rememberSavable(permissionRequester, canStart)
        }
        AskingStrategy.KEEP_ASKING -> {
            KeepAskingStrategy.rememberSavable(permissionRequester, canStart)
        }
        AskingStrategy.ONLY_ASK_SYSTEM -> {
            OnlyAskSystem.rememberSavable(permissionRequester, canStart)
        }
        else -> {
            customFactory?.invoke(type, permissionRequester, canStart)
                ?: throw UndefinedAskingStrategy(type)
        }
    }
}

/**
 * Convenience function that works like a factory to create and [remember] the proper [UserDrivenAskingStrategy]
 * based on the supplied [AskingStrategy].
 *
 * You can pass your custom factory in [customFactory] parameter.
 *
 * @param context Context used to create [AndroidPermissionRequester] instance.
 * @param type [AskingStrategy] which defines the [UserDrivenAskingStrategy] that will be created.
 * @param permissions A [List] of [String] that represents android permissions declared on Manifest
 * and will be used in the [UserDrivenAskingStrategy] creation
 * @param canStart Lambda that will be used in the [UserDrivenAskingStrategy] creation, as defined in contract.
 * @param customFactory Optional lambda that creates custom made instances of [UserDrivenAskingStrategy]
 *
 * @return [UserDrivenAskingStrategy]
 *
 * @see [AskingStrategy],[UserDrivenAskingStrategy],[PermissionRequester],[AndroidPermissionRequester]
 */
@Composable
fun rememberUserDrivenAskingStrategy(
    context: Context = LocalContext.current,
    type: AskingStrategy = AskingStrategy.STOP_ASKING_ON_USER_DENIAL,
    permissions: List<String>,
    canStart: () -> Boolean,
    customFactory: ((
        type: AskingStrategy,
        permissionRequester: PermissionRequester<List<String>, String>,
        canStart: () -> Boolean
    ) -> UserDrivenAskingStrategy<Map<String, Boolean>>)? = null
): UserDrivenAskingStrategy<Map<String, Boolean>> {
    return rememberUserDrivenAskingStrategy(
        type = type,
        canStart = canStart,
        permissionRequester = AndroidPermissionRequester.rememberAndroidPermissionRequester(
            context = context,
            permissions = permissions
        ),
        customFactory = customFactory
    )
}

/**
 * Convenience [AskingStrategy] extension function that works like a factory to create and [remember]
 * the proper [UserDrivenAskingStrategy] based on the supplied [AskingStrategy].
 *
 * You can pass your custom factory in [customFactory] parameter.
 *
 * @receiver [AskingStrategy]
 * @param permissionRequester [PermissionRequester] that will be used in the [UserDrivenAskingStrategy]
 * creation
 * @param canStart Lambda that will be used in the [UserDrivenAskingStrategy] creation, as defined in contract.
 * @param customFactory Optional lambda that creates custom made instances of [UserDrivenAskingStrategy]
 *
 * @return [UserDrivenAskingStrategy]
 *
 * @see [AskingStrategy],[UserDrivenAskingStrategy],[PermissionRequester],[rememberUserDrivenAskingStrategy]
 */
@JvmName("rememberUserDrivenAskingStrategy1")
@Composable
fun AskingStrategy.rememberUserDrivenAskingStrategy(
    permissionRequester: PermissionRequester<List<String>, String>,
    canStart: () -> Boolean,
    customFactory: ((
        type: AskingStrategy,
        permissionRequester: PermissionRequester<List<String>, String>,
        canStart: () -> Boolean
    ) -> UserDrivenAskingStrategy<Map<String, Boolean>>)? = null
): UserDrivenAskingStrategy<Map<String, Boolean>> =

    rememberUserDrivenAskingStrategy(
        type = this,
        permissionRequester = permissionRequester,
        canStart = canStart,
        customFactory
    )

/**
 * Exception used in [AskingStrategy] factories when it cannot create an instance of [UserDrivenAskingStrategy]
 * based on the supplied [AskingStrategy]
 *
 * @param askingStrategy the uncreatable [UserDrivenAskingStrategy] type.
 *
 * @see [rememberUserDrivenAskingStrategy]
 */
class UndefinedAskingStrategy(askingStrategy: AskingStrategy) :
    Throwable("Undefined Asking Strategy ${askingStrategy.name} or custom factory not provided.")