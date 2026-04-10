package net.flipper.bridge.connection.service.mapper

import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.service.model.ConnectionAction
import net.flipper.bridge.connection.service.model.ExpectedState
import net.flipper.core.busylib.log.info

internal object ConnectionStatusMapper {
    fun mapAction(
        expectedState: ExpectedState,
        realState: FDeviceConnectStatus
    ): ConnectionAction {
        info { "expectedState: $expectedState, realState: $realState" }
        return when (realState) {
            is FDeviceConnectStatus.Connected -> when (expectedState) {
                is ExpectedState.Connected -> {
                    if (expectedState.device != realState.device) {
                        ConnectionAction.Connect(expectedState.device)
                    } else {
                        ConnectionAction.Skip
                    }
                }

                ExpectedState.Disconnected -> ConnectionAction.Disconnect
            }

            is FDeviceConnectStatus.Disconnected -> when (expectedState) {
                is ExpectedState.Connected -> ConnectionAction.Connect(expectedState.device)
                ExpectedState.Disconnected -> ConnectionAction.Skip
            }

            is FDeviceConnectStatus.Connecting -> when (expectedState) {
                is ExpectedState.Connected -> if (expectedState.device != realState.device) {
                    ConnectionAction.Connect(expectedState.device)
                } else {
                    ConnectionAction.Skip
                }

                ExpectedState.Disconnected -> ConnectionAction.Disconnect
            }

            is FDeviceConnectStatus.Disconnecting -> ConnectionAction.Skip
        }
    }
}
