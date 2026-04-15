package net.flipper.bridge.connection.orchestrator.api.model

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.wrapper.WrappedNonEmptyList
import net.flipper.core.busylib.data.NonEmptyList

sealed interface FDeviceConnectStatus {
    data class Disconnected(
        val device: BUSYBar?,
        val reason: DisconnectStatus
    ) : FDeviceConnectStatus

    sealed interface Connecting : FDeviceConnectStatus {
        val device: BUSYBar
        val status: ConnectingStatus
        val transportTypes: WrappedNonEmptyList<FDeviceTransportType>

        data class InProgress(
            override val device: BUSYBar,
            override val status: ConnectingStatus,
            override val transportTypes: WrappedNonEmptyList<FDeviceTransportType>
        ) : Connecting

        data class Offline(
            override val device: BUSYBar,
            override val status: ConnectingStatus,
            override val transportTypes: WrappedNonEmptyList<FDeviceTransportType>
        ) : Connecting {
            val uiOfflineBarStatus: UIOfflineBarStatus
                get() = if (transportTypes.origin.contains(FDeviceTransportType.CLOUD)) {
                    UIOfflineBarStatus.NO_CLOUD_CONNECTED
                } else {
                    UIOfflineBarStatus.NOT_CONNECTED
                }
        }
    }

    data class Disconnecting(
        val device: BUSYBar
    ) : FDeviceConnectStatus

    class Connected(
        val scope: CoroutineScope,
        val device: BUSYBar,
        val deviceApi: FConnectedDeviceApi,
        val transportType: FDeviceTransportType
    ) : FDeviceConnectStatus
}
