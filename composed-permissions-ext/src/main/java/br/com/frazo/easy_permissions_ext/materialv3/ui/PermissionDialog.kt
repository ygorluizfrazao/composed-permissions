package br.com.frazo.easy_permissions_ext.materialv3.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import br.com.frazo.easy_permissions.base.PermissionsAskingStrategyInteraction
import br.com.frazo.easy_permissions.base.providers.android.AndroidPermissionProvider.Companion.toHumanLanguage
import br.com.frazo.easy_permissions.base.strategy.PermissionFlowStateEnum

data class PermissionDialogProperties(
    val shape: Shape,
    val dialogPaddingValues: PaddingValues,
    val verticalArrangement: Arrangement.HorizontalOrVertical,
    val title: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String,
    val titleStyle: TextStyle,
    val message: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String,
    val messageStyle: TextStyle,
    val grantButtonText: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String,
    val denyButtonText: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String,
    val onGrantButtonClicked: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit,
    val onDenyButtonClicked: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit
)

@Composable
fun createPermissionDialogProperties(
    shape: Shape = ShapeDefaults.Large,
    dialogPaddingValues: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(8.dp),
    title: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String =
        { _, permissionsMap ->
            val notGranted = permissionsMap.filter { (_, granted) -> !granted }
            if (notGranted.size > 1) "Permissions Required" else "Permission Required"
        },
    titleStyle: TextStyle = LocalTextStyle.current.copy(
        fontWeight = FontWeight.Bold
    ),
    message: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String = { _, permissionsStatus: Map<String, Boolean> ->
        val notGranted = permissionsStatus.filter { (_, granted) -> !granted }
        val notGrantedString =
            notGranted.map { (permission, _) -> permission.toHumanLanguage() + "\n" }
                .reduce { acc, s ->
                    acc + s
                }
        if (notGranted.size > 1) {
            "To use this feature, the following permissions are required:\n$notGrantedString"
        } else {
            "To use this feature, the following permission is required:\n$notGrantedString"
        }
    },
    messageStyle: TextStyle = LocalTextStyle.current,
    grantButtonText: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String = { _, _ -> "Grant" },
    denyButtonText: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> String = { _, _ -> "Deny" },
    onGrantButtonClicked: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit = { _, _ -> },
    onDenyButtonClicked: (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit = { _, _ -> }

): PermissionDialogProperties {
    return PermissionDialogProperties(
        shape,
        dialogPaddingValues,
        verticalArrangement,
        title,
        titleStyle,
        message,
        messageStyle,
        grantButtonText,
        denyButtonText,
        onGrantButtonClicked,
        onDenyButtonClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDialog(
    permissionDialogProperties: PermissionDialogProperties,
    state: PermissionFlowStateEnum,
    permissionsStatus: Map<String, Boolean>,
    callMeWhen: PermissionsAskingStrategyInteraction,
    context: Context = LocalContext.current
) {
    val title = remember {
        derivedStateOf {
            permissionDialogProperties.title(state, permissionsStatus)
        }
    }

    val message = remember {
        derivedStateOf {
            permissionDialogProperties.message(state, permissionsStatus)
        }
    }

    val grantButtonText = remember {
        derivedStateOf {
            permissionDialogProperties.grantButtonText(state, permissionsStatus)
        }
    }

    val denyButtonText = remember {
        derivedStateOf {
            permissionDialogProperties.denyButtonText(state, permissionsStatus)
        }
    }

    AlertDialog(
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        onDismissRequest = {
            callMeWhen.manuallyDeniedByUser()
        }) {
        Surface(shape = permissionDialogProperties.shape) {
            Column(
                modifier = Modifier.padding(permissionDialogProperties.dialogPaddingValues),
                verticalArrangement = permissionDialogProperties.verticalArrangement
            ) {

                Text(
                    text = title.value,
                    style = permissionDialogProperties.titleStyle,
                )
                Divider()
                Text(
                    text = message.value,
                    style = permissionDialogProperties.messageStyle
                )
                Divider()
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(onClick = {
                        callMeWhen.requestedUserManualGrant()
                        context.goToAppSettings()
                        permissionDialogProperties.onGrantButtonClicked(
                            state,
                            permissionsStatus
                        )
                    }) {
                        Text(text = grantButtonText.value)
                    }
                    OutlinedButton(onClick = {
                        callMeWhen.manuallyDeniedByUser()
                        permissionDialogProperties.onDenyButtonClicked(
                            state,
                            permissionsStatus
                        )
                    }) {
                        Text(text = denyButtonText.value)
                    }
                }
            }
        }
    }
}

fun Context.goToAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also { startActivity(it) }
}