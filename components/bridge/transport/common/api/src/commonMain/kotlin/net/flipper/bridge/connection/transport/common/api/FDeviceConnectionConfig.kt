package net.flipper.bridge.connection.transport.common.api

import net.flipper.core.busylib.data.NonEmptyList

abstract class FDeviceConnectionConfig<T : FConnectedDeviceApi> {
    abstract val uniqueId: String

    abstract fun getTransportTypes(): NonEmptyList<FInternalTransportConnectionType>
}
