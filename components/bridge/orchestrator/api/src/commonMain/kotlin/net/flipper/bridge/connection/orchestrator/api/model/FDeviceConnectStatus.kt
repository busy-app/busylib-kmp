package net.flipper.bridge.connection.orchestrator.api.model

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi

sealed class FDeviceConnectStatus {
    data class Disconnected(
        val device: BUSYBar?,
        val reason: DisconnectStatus
    ) : FDeviceConnectStatus()

    data class Connecting(
        val device: BUSYBar,
        val status: ConnectingStatus
    ) : FDeviceConnectStatus()

    data class Disconnecting(
        val device: BUSYBar
    ) : FDeviceConnectStatus()

    class Connected(
        val scope: CoroutineScope,
        val device: BUSYBar,
        val deviceApi: FConnectedDeviceApi
    ) : FDeviceConnectStatus()
}
