package net.flipper.bridge.connection.orchestrator.api.model

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.config.api.model.FDeviceCombined
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi

sealed class FDeviceConnectStatus {
    data class Disconnected(
        val device: FDeviceCombined?,
        val reason: DisconnectStatus
    ) : FDeviceConnectStatus()

    data class Connecting(
        val device: FDeviceCombined,
        val status: ConnectingStatus
    ) : FDeviceConnectStatus()

    data class Disconnecting(
        val device: FDeviceCombined
    ) : FDeviceConnectStatus()

    class Connected(
        val scope: CoroutineScope,
        val device: FDeviceCombined,
        val deviceApi: FConnectedDeviceApi
    ) : FDeviceConnectStatus()
}
