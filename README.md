# easy-permissions

## What is it?
A Convenience library aimed to help dealing with Android Compose permissions system in a way to avoid extra unecessary complexity and coupling.

## Why it exists?
It is always a pain to deal with permissions in android, thats specially true in compose, with states, recomposition and all. Suddenly, your viewmodel logic will contain a bunch of extra code, flow controls, conditionals, etc. Just to deal with a platform specific logic.
I believe ViewModels exist to proper translate you business logic to the platform UI and vice versa, not to deal with very very speific stuff.

## When should i use it?
Whenever you want:

- Worry about your requirements, not Android's.
- Less coupling.
- Less boilerplate.
- Dealing with `shouldShowRequestPermissionRationale`.
- Atomize your permissions requests.

## How it works?
Just do what you would do in your code, compose aware in a lambda block. 

### Define what you want to use and when you want to use it:
The main entry point of this library is the Composable Lambda `WithPermission`.

```kotlin
@Composable
fun WithPermission(
    userDrivenAskingStrategy: UserDrivenAskingStrategy<Map<String, Boolean>>,
    initialStateContent: @Composable () -> Unit,
    rationalePrompt: @Composable (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>, callMeWhen: PermissionsAskingStrategyInteraction) -> Unit,
    terminalStateContent: @Composable (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit
)
```

* `userDrivenAskingStrategy`: Contract that defines how the process of permission handling will occur.
* `initialStateContent`: How is your content before  `userDrivenAskingStrategy` reaches a terminal state?
* `rationalePrompt`: How is your content when Android denies the permission?
  - `state: PermissionFlowStateEnum`: Tell at which state the underlying `userDrivenAskingStrategy` is. it can be:
    - `NOT_STARTED` : The strategy is in its initial state.
    - `STARTED` : Started processing.
    - `DENIED_BY_SYSTEM` : The permission request was denied by Android (Who would have guessed, huh).
    - `APP_PROMPT` : Waiting for app handled prompt (`rationalePrompt`).
    - `TERMINAL_GRANTED` : All permissions were granted.
    - `TERMINAL_DENIED` : Not all permissions were granted.
  - `permissionsStatus: Map<String, Boolean>` : Current status of the required permissions.
  - `callMeWhen: PermissionsAskingStrategyInteraction` : Since you will handle the `rationalePrompt` you have to inform the state machine about the user interaction, the, you call its methods in the apropriate case.
* `terminalStateContent`: How is your content after the process finishes? (Can succed or fail, you will know about this in the `permissionsStatus` parameter)
  - It's params have the same logic as stated in the previous item.

#### Wait, i don't want to deal with this `rationalePrompt` stuff, it is still boilerplate and i feel really lazy now...
Then, my dear friend, import the `ext`dependency too. It has a function that abstracts this bit of code inside a *Materialv3* `AlertDialog`, you will just have to pass some params to create it.

This.
```kotlin
@Composable
fun WithPermission(
    userDrivenAskingStrategy: UserDrivenAskingStrategy<Map<String, Boolean>>,
    permissionDialogProperties: PermissionDialogProperties = createPermissionDialogProperties(),
    initialStateContent: @Composable () -> Unit,
    terminalStateContent: @Composable (state: PermissionFlowStateEnum, permissionsStatus: Map<String, Boolean>) -> Unit
) 
```
And that.
```kotlin
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
```
#### Wait, what?! Why so many parameters...?
So you can localize and theme your dialog apropriately, but, if you don't want the headache, you can use:
```kotlin
createPermissionDialogProperties()
```
and change only what you want to change.

**IMPORTANT NOTE**
You should pass at least `onGrantButtonClicked` to do something in your app when the user wants to grante the permission manually, an example would be to open the app settings, like this:

```kotlin
fun Context.goToAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also { startActivity(it) }
}
```

```kotlin
permissionDialogProperties = createPermissionDialogProperties(
                    onGrantButtonClicked = { _, _ ->
                        context.goToAppSettings()
                        clicked = false
                    },
                )
```
### An example:

Here is an app of notes which needs permission to record audio and location in very specific place.

<img src="example/images/example.gif" alt="A notes app demo" style="width:20%; height:20%">

1. Create and remember your `UserDrivenAskingStrategy`:
```kotlin
    var canStart by rememberSaveable {
        mutableStateOf(false)
    }

    val userDrivenAskingStrategy =
        rememberUserDrivenAskingStrategy(
            type = AskingStrategy.STOP_ASKING_ON_USER_DENIAL,
            permissions = listOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            canStart = { canStart }
        )
```
**IMPORTANT NOTE**
The `canStart: () -> Boolean` param tells the `UserDrivenAskingStrategy` when it should start asking. It is `{true}` by default, which means, it will start the process as soon as the composition happens. In the above example, we want to control the start of the flow, so, we pass our own implementation.

2. Call `WithPermission` "with" the controls that depends on specific permissions.

```kotlin
    NotesList(...) {
        Row(...) {
            WithPermission(
                userDrivenAskingStrategy = userDrivenAskingStrategy,
                permissionDialogProperties = createPermissionDialogProperties(
                    onGrantButtonClicked = { _, _ ->
                        context.goToAppSettings()
                        canStart = false
                    },
                ),
                initialStateContent = {
                    IconButton(onClick = { canStart = true }) {
                        IconResource.fromImageVector(
                            Icons.Default.Warning,
                            ""
                        ).ComposeIcon()
                    }
                }) { _, permissionMap ->

                val permissionsMapState = remember {
                    mutableStateOf(permissionMap)
                }

                val grantedPermissions = remember {
                    derivedStateOf {
                        permissionsMapState.value.filter { (_, granted) -> granted }
                    }
                }

                val deniedPermissions = remember {
                    derivedStateOf {
                        permissionsMapState.value.filter { (_, granted) -> !granted }
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                var showPopup by remember {
                    mutableStateOf(false)
                }

                if (deniedPermissions.value.isEmpty()) {
                    IconButton(onClick = {
                        showPopup = true
                        coroutineScope.launch {
                            Toast.makeText(
                                context,
                                "Call a fancy ViewModel Action!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }) {
                        IconResource.fromImageVector(
                            Icons.Default.CheckCircle,
                            ""
                        ).ComposeIcon()
                    }
                } else {
                    IconButton(onClick = {
                        showPopup = true
                        coroutineScope.launch {
                            Toast.makeText(
                                context,
                                "Granted: ${grantedPermissions.value}\nDenied: ${deniedPermissions.value}",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }) {
                        IconResource.fromImageVector(
                            Icons.Default.Cancel,
                            ""
                        ).ComposeIcon()
                    }
                }

                if (showPopup) {
                    Popup(
                        properties = PopupProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                            focusable = true
                        ),
                        onDismissRequest = { showPopup = false },
                        popupPositionProvider = object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize
                            ): IntOffset {
                                return IntOffset(
                                    (windowSize.width - popupContentSize.width) / 2,
                                    (windowSize.height - popupContentSize.height) / 2
                                )
                            }
                        }) {
                        Surface(
                            border = BorderStroke(1.dp, LocalContentColor.current),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                            ) {
                                Text(
                                    text = "Granted Permissions: \n${
                                        grantedPermissions.value.map { it.key.toHumanLanguage() + "\n" }
                                            .joinToString(separator = "")
                                    }"
                                )

                                Divider()

                                Text(
                                    text = "Denied Permissions: \n${
                                        deniedPermissions.value.map { it.key.toHumanLanguage() + "\n" }
                                            .joinToString(separator = "")
                                    }"
                                )
                            }
                        }
                    }
                }
            }
            ...
        }
    }
```

And that's it, everything will be automated for you. If this is not enough for you, check the other classes and functions in the package, almost all of them have their KDOC's documented.

### Under the hood:
It uses a finite state machine to track at which point of a process the user is. Inside these state machines we have a StateFlow variable. When this stateflow variable updates, the UI recompose. Also, it resolves the state every time lifecycle is resumed.

## Other notes:
This is very initial and little test was done, please, help me improve it.
Also, you can whatever you want with it, it's free to use, modify, copy, paste and distribute, just remember to give the credits.

If you want, can, and feels like it, you can 

[<img src="http://www.google.com.au/images/nav_logo7.png">](http://google.com.au/)
