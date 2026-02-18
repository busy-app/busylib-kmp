package net.flipper.bridge.connection.transport.combined.impl.connections.helpers

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

/**
 * Test configuration for connection tests.
 *
 * @property id Unique identifier for distinguishing configs in tests
 */
data class TestConfig(
    val id: String = ""
) : FDeviceConnectionConfig<TestConnectedDeviceApi>()
