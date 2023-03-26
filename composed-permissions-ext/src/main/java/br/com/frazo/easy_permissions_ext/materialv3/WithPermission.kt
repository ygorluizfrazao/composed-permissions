package br.com.frazo.easy_permissions_ext.materialv3

import androidx.compose.runtime.Composable
import br.com.frazo.easy_permissions.base.PermissionsAskingStrategyInteraction
import br.com.frazo.easy_permissions.base.strategy.PermissionFlowStateEnum
import br.com.frazo.easy_permissions.base.strategy.UserDrivenAskingStrategy
import br.com.frazo.easy_permissions_ext.materialv3.ui.PermissionDialog
import br.com.frazo.easy_permissions_ext.materialv3.ui.PermissionDialogProperties
import br.com.frazo.easy_permissions_ext.materialv3.ui.createPermissionDialogProperties

@Composable
fun WithPermission(
    userDrivenAskingStrategy: UserDrivenAskingStrategy<Map<String, Boolean>>,
    permissionDialogProperties: PermissionDialogProperties = createPermissionDialogProperties(),
    initialStateContent: @Composable () -> Unit,
    terminalStateContent: @Composable (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit
) {
    val dialog: @Composable (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>, callMeWhen: PermissionsAskingStrategyInteraction) -> Unit =
        { state, permissionsStatus, callMeWhen ->
            PermissionDialog(
                permissionDialogProperties = permissionDialogProperties,
                state = state,
                permissionsStatus = permissionsStatus,
                callMeWhen = callMeWhen
            )
        }

    br.com.frazo.easy_permissions.base.WithPermission(
        userDrivenAskingStrategy = userDrivenAskingStrategy,
        initialStateContent = initialStateContent,
        rationalePrompt = dialog,
        terminalStateContent = terminalStateContent
    )
}