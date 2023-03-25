package br.com.frazo.easy_permissions.base.strategy

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import br.com.frazo.easy_permissions.base.requesters.PermissionRequester
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [UserDrivenAskingStrategy] implementation that will give up the acquisition of a permission when the
 * user denies inside the application scope.
 *
 * It can assume the following [states]([PermissionFlowStateEnum]):
 *
 * [NOT_STARTED]([PermissionFlowStateEnum.NOT_STARTED])
 *
 * [STARTED]([PermissionFlowStateEnum.STARTED])
 *
 * [DENIED_BY_SYSTEM]([PermissionFlowStateEnum.DENIED_BY_SYSTEM])
 *
 * [APP_PROMPT]([PermissionFlowStateEnum.APP_PROMPT])
 *
 * [TERMINAL_GRANTED]([PermissionFlowStateEnum.TERMINAL_GRANTED])
 *
 * [TERMINAL_DENIED]([PermissionFlowStateEnum.TERMINAL_DENIED])
 *
 * @param permissionRequester [PermissionRequester] that will be used to ask the underlying platform
 * about a permission.
 * @param canStart Lambda function that tells the machine when it should be running. usually the creator
 * supply a [State]([androidx.compose.runtime.State]) that changes on user interaction with UI. the default value
 * is always return true.
 */
class StopOnUserDenialAskingStrategy(
    private val permissionRequester: PermissionRequester<List<String>, String>,
    private val canStart: () -> Boolean = { true },
) :
    UserDrivenAskingStrategy<Map<String, Boolean>> {

    /**
     * Secondary constructor used to restore an state when it has been saved due to application platform
     * management of resources, as in, Android Configuration Change events.
     *
     * @param permissionRequester [PermissionRequester] that will be used to ask the underlying platform
     * about a permission.
     * @param canStart Lambda function that tells the machine when it should be running. usually the creator
     * supply a [State]([androidx.compose.runtime.State]) that changes on user interaction with UI. the default value
     * is always return true.
     * @param savedData [DataHolder] containing the relevant data to be recovered.
     */
    private constructor(
        permissionRequester: PermissionRequester<List<String>, String>,
        canStart: () -> Boolean = { true },
        savedData: DataHolder
    ) : this(permissionRequester, canStart) {
        _flowState.value =
            PermissionFlowState(savedData.state, permissionRequester.permissionsStatus())
        userManuallyDenied = savedData.userManuallyDenied
    }

    /**
     * Inner data class that holds relevant information used in recovery of an [StopOnUserDenialAskingStrategy] instance.
     */
    private data class DataHolder(
        val state: PermissionFlowStateEnum,
        val userManuallyDenied: Boolean
    )

    /**
     * A flow of states [this strategy]([KeepAskingStrategy]) can assume.
     */
    private val _flowState =
        MutableStateFlow(
            PermissionFlowState(
                PermissionFlowStateEnum.NOT_STARTED,
                emptyMap<String, Boolean>()
            )
        )

    /**
     * A control flag that informs when the user has denied the permission inside the application scope.
     */
    private var userManuallyDenied = false

    /**
     * A counter that tracks how many times the [permissionRequester] has been asked for permissions.
     */
    private var requestsMade = 0

    /**
     * Flow control flag that ensures multiple consecutive calls to [permissionRequester] ask are not
     * issued.
     */
    private var waitingRequesterResponse = false

    /**
     * Flow control flag that ensures multiple changes to [APP_PROMPT]([PermissionFlowStateEnum.APP_PROMPT])
     * are not issued. When used with Compose this guarantee just one recomposition.
     */
    private var waitingUserResponse = false

    /**
     * A map that serves as a cache for the latest permission request result.
     */
    private var lastPermissionMap = emptyMap<String, Boolean>()

    /**
     * In [this case]([StopOnUserDenialAskingStrategy]) sets [userManuallyDenied] flag to true and assume
     * [TERMINAL_DENIED]([PermissionFlowStateEnum.TERMINAL_DENIED]).
     *
     * @see UserDrivenAskingStrategy.onUserManuallyDenied
     */
    override fun onUserManuallyDenied() {
        waitingUserResponse = false
        userManuallyDenied = true
        assignNewState(
            PermissionFlowState(
                PermissionFlowStateEnum.TERMINAL_DENIED,
                lastPermissionMap
            )
        )
    }

    /**
     * In [this case]([StopOnUserDenialAskingStrategy]) resets [waitingRequesterResponse] flag to false and
     * [restarts]([PermissionFlowStateEnum.NOT_STARTED]).
     *
     * @see UserDrivenAskingStrategy.onRequestedUserManualGrant
     */
    override fun onRequestedUserManualGrant() {
        waitingUserResponse = false
        assignNewState(
            PermissionFlowState(
                PermissionFlowStateEnum.NOT_STARTED,
                lastPermissionMap
            )
        )
    }

    /**
     * Implementation of [PermissionAskingStrategy.resolveState].
     * In [this case]([StopOnUserDenialAskingStrategy]) will give up the acquisition of a permission
     * when the user denies the permission in the application scope.
     */
    override fun resolveState() {
        lastPermissionMap = permissionRequester.permissionsStatus()

        if (lastPermissionMap.filter { (_, isGranted) ->
                !isGranted
            }.isEmpty() && _flowState.value.state != PermissionFlowStateEnum.TERMINAL_GRANTED) {
            assignNewState(
                PermissionFlowState(
                    PermissionFlowStateEnum.TERMINAL_GRANTED,
                    lastPermissionMap
                )
            )
        } else {
            when (_flowState.value.state) {
                PermissionFlowStateEnum.NOT_STARTED -> {
                    if (canStart()) {
                        requestsMade = 0
                        userManuallyDenied = false
                        assignNewState(
                            PermissionFlowState(
                                PermissionFlowStateEnum.STARTED,
                                lastPermissionMap
                            )
                        )
                    }
                }

                PermissionFlowStateEnum.STARTED -> {
                    if (requestsMade == 0) {
                        if (!waitingRequesterResponse) {
                            waitingRequesterResponse = true
                            permissionRequester.ask { permissionsMap ->
                                waitingRequesterResponse = false
                                requestsMade++
                                val notGranted = permissionsMap.filter { (_, isGranted) ->
                                    !isGranted
                                }
                                if (notGranted.isEmpty()) {
                                    assignNewState(
                                        PermissionFlowState(
                                            PermissionFlowStateEnum.TERMINAL_GRANTED,
                                            permissionsMap
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        assignNewState(_flowState.value.copy(state = PermissionFlowStateEnum.DENIED_BY_SYSTEM))
                    }
                }

                PermissionFlowStateEnum.DENIED_BY_SYSTEM, PermissionFlowStateEnum.APP_PROMPT -> {
                    if (!userManuallyDenied && !waitingUserResponse && !waitingRequesterResponse) {
                        waitingUserResponse = true
                        assignNewState(
                            PermissionFlowState(
                                PermissionFlowStateEnum.APP_PROMPT,
                                lastPermissionMap
                            )
                        )
                    }
                }

                PermissionFlowStateEnum.TERMINAL_GRANTED -> {
                    if (lastPermissionMap.filter { (_, isGranted) ->
                            !isGranted
                        }.isNotEmpty()) {
                        assignNewState(
                            PermissionFlowState(
                                PermissionFlowStateEnum.NOT_STARTED,
                                lastPermissionMap
                            )
                        )
                    }
                }

                PermissionFlowStateEnum.TERMINAL_DENIED -> Unit
            }
        }
    }

    /**
     * Implementation of [PermissionAskingStrategy.flowState]
     *
     * @return [_flowState] as a [StateFlow]
     */
    override fun flowState(): Flow<PermissionFlowState<Map<String, Boolean>>> {
        return _flowState.asStateFlow()
    }

    /**
     * Convenience method to to assign a new state an log to the console.
     *
     * @return [_flowState] as a [StateFlow]
     */
    private fun assignNewState(state: PermissionFlowState<Map<String, Boolean>>) {
        _flowState.value = state
        Log.d(this::class.java.name, state.toString())
    }

    companion object {

        /**
         * Creates and remember a [StopOnUserDenialAskingStrategy] that survives configuration changes.
         */
        @Composable
        fun rememberSavable(
            permissionRequester: PermissionRequester<List<String>, String>,
            canStart: () -> Boolean
        ): UserDrivenAskingStrategy<Map<String, Boolean>> {
            return rememberSaveable(
                permissionRequester,
                canStart,
                saver = saver(permissionRequester, canStart)
            ) {
                StopOnUserDenialAskingStrategy(
                    permissionRequester,
                    canStart
                )
            }
        }

        /**
         * Returns a [Saver] to properly store the state inside a [rememberSaveable].
         *
         * @return [Saver]<[StopOnUserDenialAskingStrategy],[Any]>
         */
        private fun saver(
            permissionRequester: PermissionRequester<List<String>, String>,
            canStart: () -> Boolean
        ): Saver<StopOnUserDenialAskingStrategy, Any> {
            val flowStateKey = "flowStateKey"
            val userManuallyDeniedKey = "userManuallyDeniedKey"

            return mapSaver(
                save = {
                    mapOf(
                        flowStateKey to it._flowState.value.state,
                        userManuallyDeniedKey to it.userManuallyDenied
                    )
                },
                restore = {
                    val data = DataHolder(
                        it[flowStateKey] as PermissionFlowStateEnum,
                        it[userManuallyDeniedKey] as Boolean
                    )
                    StopOnUserDenialAskingStrategy(permissionRequester, canStart, data)
                }
            )
        }

    }

}