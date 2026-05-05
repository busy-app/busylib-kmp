package net.flipper.bridge.connection.transport.common.api

import kotlinx.coroutines.CoroutineScope
import net.flipper.core.busylib.data.NonEmptyList
import net.flipper.core.busylib.data.nonEmptyListOf

sealed class FInternalTransportConnectionStatus {
    data class Disconnected(
        val reason: FInternalDisconnectedReason
    ) : FInternalTransportConnectionStatus()

    data class Connecting(
        val connectionTypes: NonEmptyList<FInternalTransportConnectionType>
    ) : FInternalTransportConnectionStatus() {
        constructor(type: FInternalTransportConnectionType) : this(nonEmptyListOf(type))
    }

    data class Connected(
        val scope: CoroutineScope,
        val deviceApi: FConnectedDeviceApi,
        val connectionTypes: NonEmptyList<FInternalTransportConnectionType>
    ) : FInternalTransportConnectionStatus() {
        constructor(
            scope: CoroutineScope,
            deviceApi: FConnectedDeviceApi,
            connectionType: FInternalTransportConnectionType
        ) : this(scope, deviceApi, nonEmptyListOf(connectionType))
    }

    data object Disconnecting : FInternalTransportConnectionStatus()
}
