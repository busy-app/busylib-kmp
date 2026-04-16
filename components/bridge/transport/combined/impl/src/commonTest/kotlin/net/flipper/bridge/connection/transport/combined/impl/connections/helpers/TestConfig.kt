package net.flipper.bridge.connection.transport.combined.impl.connections.helpers

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.core.busylib.data.NonEmptyList
import net.flipper.core.busylib.data.nonEmptyListOf

/**
 * Test configuration for connection tests.
 *
 * @property id Unique identifier for distinguishing configs in tests
 */
data class TestConfig(
    val id: String = ""
) : FDeviceConnectionConfig<TestConnectedDeviceApi>() {
    override fun getTransportTypes(): NonEmptyList<FInternalTransportConnectionType> {
        return nonEmptyListOf(FInternalTransportConnectionType.MOCK)
    }
}
