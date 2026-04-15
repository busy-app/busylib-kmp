package net.flipper.bridge.connection.orchestrator.api.model

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi

sealed interface FDeviceConnectStatus {
    data class Disconnected(
        val device: BUSYBar?,
        val reason: DisconnectStatus
    ) : FDeviceConnectStatus

    sealed interface Connecting : FDeviceConnectStatus {
        val device: BUSYBar
        val status: ConnectingStatus

        data class InProgress(
            override val device: BUSYBar,
            override val status: ConnectingStatus
        ) : Connecting

        data class Offline(
            override val device: BUSYBar,
            override val status: ConnectingStatus
        ) : Connecting
    }

    data class Disconnecting(
        val device: BUSYBar
    ) : FDeviceConnectStatus

    class Connected(
        val scope: CoroutineScope,
        val device: BUSYBar,
        val deviceApi: FConnectedDeviceApi,
        val transportType: FDeviceTransportType?
    ) : FDeviceConnectStatus
}
