package net.flipper.bridge.connection.orchestrator.api.model

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi

sealed class FDeviceConnectStatus {
    data class Disconnected(
        val device: FDeviceBaseModel?,
        val reason: DisconnectStatus
    ) : FDeviceConnectStatus()

    data class Connecting(
        val device: FDeviceBaseModel,
        val status: ConnectingStatus
    ) : FDeviceConnectStatus()

    data class Disconnecting(
        val device: FDeviceBaseModel
    ) : FDeviceConnectStatus()

    class Connected(
        val scope: CoroutineScope,
        val device: FDeviceBaseModel,
        val deviceApi: FConnectedDeviceApi
    ) : FDeviceConnectStatus()
}
