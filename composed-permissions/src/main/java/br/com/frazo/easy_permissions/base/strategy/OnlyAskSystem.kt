package br.com.frazo.easy_permissions.base.strategy

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import br.com.frazo.easy_permissions.base.requesters.PermissionRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [UserDrivenAskingStrategy] implementation that gives up the acquisition of a permission once the system denies.
 *
 * It can assume the following [states]([PermissionFlowStateEnum]):
 *
 * [NOT_STARTED]([PermissionFlowStateEnum.NOT_STARTED])
 *
 * [STARTED]([PermissionFlowStateEnum.STARTED])
 *
 * [TERMINAL_DENIED]([PermissionFlowStateEnum.TERMINAL_DENIED])
 *
 * [TERMINAL_GRANTED]([PermissionFlowStateEnum.TERMINAL_GRANTED])
 *
 * Be wary, it it uses multiple permissions, and just some are denied, it will ask about the ones that are not denied,
 * and assumes a [TERMINAL_DENIED]([PermissionFlowStateEnum.TERMINAL_DENIED]) state in the end anyway.
 *
 * @param permissionRequester [PermissionRequester] that will be used to ask the underlying platform
 * about a permission.
 * @param canStart Lambda function that tells the machine when it should be running. usually the creator
 * supply a [State]([androidx.compose.runtime.State]) that changes on user interaction with UI. the default value
 * is always return true.
 */
class OnlyAskSystem(
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
    }

    /**
     * Inner data class that holds relevant information used in recovery of an [OnlyAskSystem] instance.
     */
    private data class DataHolder(
        val state: PermissionFlowStateEnum,
    )

    /**
     * A flow of states [this strategy]([OnlyAskSystem]) can assume.
     */
    private val _flowState =
        MutableStateFlow(
            PermissionFlowState(
                PermissionFlowStateEnum.NOT_STARTED,
                emptyMap<String, Boolean>()
            )
        )

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
     * A map that serves as a cache for the latest permission request result.
     */
    private var lastPermissionMap = emptyMap<String, Boolean>()

    /**
     * Does nothing
     */
    override fun onUserManuallyDenied() {
    }

    /**
     * Does nothing
     */
    override fun onRequestedUserManualGrant() {
    }

    /**
     * Implementation of [PermissionAskingStrategy.resolveState].
     * In [this case]([OnlyAskSystem]) it will
     * ask the requester at least twice, before giving up.
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
                        assignNewState(
                            PermissionFlowState(
                                PermissionFlowStateEnum.STARTED,
                                lastPermissionMap
                            )
                        )
                    }
                }

                PermissionFlowStateEnum.STARTED -> {
                    if (requestsMade <= 1) {
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
                                } else {
                                    assignNewState(
                                        PermissionFlowState(
                                            PermissionFlowStateEnum.TERMINAL_DENIED,
                                            permissionsMap
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                PermissionFlowStateEnum.DENIED_BY_SYSTEM, PermissionFlowStateEnum.APP_PROMPT -> {
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
    override fun flowState(): StateFlow<PermissionFlowState<Map<String, Boolean>>> {
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
         * Creates and remember a [OnlyAskSystem] that survives configuration changes.
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
                OnlyAskSystem(
                    permissionRequester,
                    canStart
                )
            }
        }

        /**
         * Returns a [Saver] to properly store the state inside a [rememberSaveable].
         *
         * @return [Saver]<[OnlyAskSystem],[Any]>
         */
        private fun saver(
            permissionRequester: PermissionRequester<List<String>, String>,
            canStart: () -> Boolean
        ): Saver<OnlyAskSystem, Any> {
            val flowStateKey = "flowStateKey"

            return mapSaver(
                save = {
                    mapOf(
                        flowStateKey to it._flowState.value.state,
                    )
                },
                restore = {
                    val data = DataHolder(
                        it[flowStateKey] as PermissionFlowStateEnum
                    )
                    OnlyAskSystem(permissionRequester, canStart, data)
                }
            )
        }

    }

}