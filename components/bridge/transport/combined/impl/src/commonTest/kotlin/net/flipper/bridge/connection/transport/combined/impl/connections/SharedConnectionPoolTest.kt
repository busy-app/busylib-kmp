package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.HalfInitializedDeviceApi
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.MockConnectionBuilder
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestHttpConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Connected
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SharedConnectionPoolTest {

    /**
     * Drives a single connection to [Connected] with [deviceApi] and returns what the pool exposes.
     */
    private suspend fun TestScope.connectedSnapshot(
        deviceApi: FConnectedDeviceApi
    ): ConnectionSnapshot {
        val connectionBuilder = MockConnectionBuilder()
        val connection = AutoReconnectConnection(
            scope = backgroundScope,
            initialConfig = TestConfig("a"),
            connectionBuilder = connectionBuilder,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        connectionBuilder.connectCalledDeferred.await()
        advanceUntilIdle()

        val pool = SharedConnectionPool(
            scope = backgroundScope,
            connectionsFlow = MutableStateFlow(listOf(connection))
        )
        advanceUntilIdle()

        connectionBuilder.latestListener()!!.onStatusUpdate(
            Connected(
                scope = backgroundScope,
                deviceApi = deviceApi,
                connectionType = FInternalTransportConnectionType.MOCK
            )
        )
        advanceUntilIdle()

        val snapshots = pool.get().first()
        assertEquals(1, snapshots.size, "Pool should expose one snapshot per connection")
        return snapshots.first()
    }

    @Test
    fun GIVEN_connected_http_device_api_WHEN_pool_is_collected_THEN_snapshot_carries_capabilities() =
        runTest {
            val capabilities = listOf(
                FHTTPTransportCapability.BB_LOCAL_CONNECTION,
                FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED
            )

            val snapshot = connectedSnapshot(
                TestHttpConnectedDeviceApi(
                    deviceName = "device",
                    capabilities = capabilities
                )
            )

            assertIs<Connected>(snapshot.status)
            assertEquals(capabilities, snapshot.capabilities)
        }

    @Test
    fun GIVEN_device_api_without_capabilities_flow_WHEN_pool_is_collected_THEN_connection_stays_usable() =
        runTest {
            val snapshot = connectedSnapshot(HalfInitializedDeviceApi())

            assertIs<Connected>(
                snapshot.status,
                "A missing capabilities flow must not hide the connection"
            )
            assertNull(snapshot.capabilities, "Capabilities are unknown, not empty")
        }
}
