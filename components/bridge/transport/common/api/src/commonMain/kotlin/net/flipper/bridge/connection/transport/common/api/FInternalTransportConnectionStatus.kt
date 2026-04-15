package net.flipper.bridge.connection.transport.common.api

import kotlinx.coroutines.CoroutineScope
import net.flipper.core.busylib.data.NonEmptyList
import net.flipper.core.busylib.data.nonEmptyListOf

sealed class FInternalTransportConnectionStatus {
    data object Disconnected : FInternalTransportConnectionStatus()

    data class Connecting(
        val connectionType: NonEmptyList<FInternalTransportConnectionType>
    ) : FInternalTransportConnectionStatus() {
        constructor(type: FInternalTransportConnectionType) : this(nonEmptyListOf(type))
    }

    data object Pairing : FInternalTransportConnectionStatus()

    data class Connected(
        val scope: CoroutineScope,
        val deviceApi: FConnectedDeviceApi,
        val connectionType: FInternalTransportConnectionType
    ) : FInternalTransportConnectionStatus()

    data object Disconnecting : FInternalTransportConnectionStatus()
}
