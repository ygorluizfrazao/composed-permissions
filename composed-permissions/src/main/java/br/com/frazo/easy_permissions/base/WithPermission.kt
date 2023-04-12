package br.com.frazo.easy_permissions.base

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.frazo.easy_permissions.base.strategy.PermissionFlowState
import br.com.frazo.easy_permissions.base.strategy.PermissionFlowStateEnum
import br.com.frazo.easy_permissions.base.strategy.UserDrivenAskingStrategy

@Composable
fun WithPermission(
    userDrivenAskingStrategy: UserDrivenAskingStrategy<Map<String, Boolean>>,
    initialStateContent: @Composable () -> Unit,
    rationalePrompt: @Composable (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>, callMeWhen: PermissionsAskingStrategyInteraction) -> Unit,
    terminalStateContent: @Composable (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit
) {

    val flowState = userDrivenAskingStrategy.flowState().collectAsState(
        initial =
        PermissionFlowState(
            PermissionFlowStateEnum.NOT_STARTED,
            emptyMap()
        )
    )

    val rationaleComposable: @Composable () -> Unit = remember {
        {
            initialStateContent()
            rationalePrompt(
                PermissionFlowStateEnum.DENIED_BY_SYSTEM,
                flowState.value.data,
                PermissionsAskingStrategyInteraction(
                    requestedUserManualGrant = {
                        userDrivenAskingStrategy.onRequestedUserManualGrant()

                    },
                    manuallyDeniedByUser = {
                        userDrivenAskingStrategy.onUserManuallyDenied()
                    })
            )
        }
    }

    userDrivenAskingStrategy.resolveState()

    when (flowState.value.state) {
        PermissionFlowStateEnum.NOT_STARTED, PermissionFlowStateEnum.STARTED, PermissionFlowStateEnum.DENIED_BY_SYSTEM -> initialStateContent()
        PermissionFlowStateEnum.APP_PROMPT -> rationaleComposable()
        PermissionFlowStateEnum.TERMINAL_GRANTED, PermissionFlowStateEnum.TERMINAL_DENIED -> terminalStateContent(
            flowState.value.state,
            flowState.value.data
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userDrivenAskingStrategy.resolveState()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    })
}

data class PermissionsAskingStrategyInteraction(
    val requestedUserManualGrant: () -> Unit,
    val manuallyDeniedByUser: () -> Unit,
)

fun Context.findActivityResultCaller(): ActivityResultCaller {
    var context = this
    while (context is ContextWrapper) {
        if (context is ActivityResultCaller) return context
        context = context.baseContext
    }
    throw IllegalStateException("no activity")
}