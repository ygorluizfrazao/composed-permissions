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



### Under the hood:
It uses a finite state machine to track at which point of a process the user is. Inside these state machines we have a StateFlow variable. Whenever this stateflow variable updates, the UI recompose.
