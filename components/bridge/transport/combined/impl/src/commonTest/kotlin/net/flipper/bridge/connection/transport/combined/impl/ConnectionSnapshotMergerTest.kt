package net.flipper.bridge.connection.transport.combined.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.flipper.bridge.connection.transport.combined.impl.connections.ConnectionSnapshot
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Connected
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Connecting
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Disconnected
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus.Disconnecting
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.core.busylib.data.nonEmptyListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConnectionSnapshotMergerTest {

    private fun testScope() = CoroutineScope(Job())

    // region mergeSnapshots - single snapshot

    @Test
    fun `mergeSnapshots with single snapshot returns it unchanged`() {
        val snapshot = ConnectionSnapshot(
            capabilities = listOf(FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED),
            status = Disconnected()
        )

        val result = mergeSnapshots(listOf(snapshot))

        assertSame(snapshot, result)
    }

    @Test
    fun `mergeSnapshots with single Connected snapshot returns it unchanged`() {
        val scope = testScope()
        val deviceApi = TestConnectedDeviceApi()
        val snapshot = ConnectionSnapshot(
            capabilities = listOf(FHTTPTransportCapability.BB_LOCAL_CONNECTION),
            status = Connected(scope, deviceApi, FInternalTransportConnectionType.LAN)
        )

        val result = mergeSnapshots(listOf(snapshot))

        assertSame(snapshot, result)
    }

    // endregion

    // region mergeSnapshots - Connected status

    @Test
    fun `mergeSnapshots combines connectionTypes from multiple Connected snapshots`() {
        val scope1 = testScope()
        val scope2 = testScope()
        val deviceApi1 = TestConnectedDeviceApi("Device1")
        val deviceApi2 = TestConnectedDeviceApi("Device2")

        val snapshots = listOf(
            ConnectionSnapshot(
                status = Connected(scope1, deviceApi1, FInternalTransportConnectionType.LAN)
            ),
            ConnectionSnapshot(
                status = Connected(scope2, deviceApi2, FInternalTransportConnectionType.CLOUD)
            )
        )

        val result = mergeSnapshots(snapshots)
        val resultStatus = assertIs<Connected>(result.status)

        assertEquals(
            listOf(FInternalTransportConnectionType.LAN, FInternalTransportConnectionType.CLOUD),
            resultStatus.connectionTypes.toList()
        )
    }

    @Test
    fun `mergeSnapshots preserves base scope and deviceApi for Connected`() {
        val scope1 = testScope()
        val scope2 = testScope()
        val deviceApi1 = TestConnectedDeviceApi("Device1")
        val deviceApi2 = TestConnectedDeviceApi("Device2")

        val snapshots = listOf(
            ConnectionSnapshot(
                status = Connected(scope1, deviceApi1, FInternalTransportConnectionType.LAN)
            ),
            ConnectionSnapshot(
                status = Connected(scope2, deviceApi2, FInternalTransportConnectionType.CLOUD)
            )
        )

        val result = mergeSnapshots(snapshots)
        val resultStatus = assertIs<Connected>(result.status)

        assertSame(scope1, resultStatus.scope)
        assertSame(deviceApi1, resultStatus.deviceApi)
    }

    @Test
    fun `mergeSnapshots deduplicates connectionTypes for Connected`() {
        val scope1 = testScope()
        val scope2 = testScope()
        val deviceApi1 = TestConnectedDeviceApi()
        val deviceApi2 = TestConnectedDeviceApi()

        val snapshots = listOf(
            ConnectionSnapshot(
                status = Connected(scope1, deviceApi1, FInternalTransportConnectionType.LAN)
            ),
            ConnectionSnapshot(
                status = Connected(scope2, deviceApi2, FInternalTransportConnectionType.LAN)
            )
        )

        val result = mergeSnapshots(snapshots)
        val resultStatus = assertIs<Connected>(result.status)

        assertEquals(
            listOf(FInternalTransportConnectionType.LAN),
            resultStatus.connectionTypes.toList()
        )
    }

    @Test
    fun `mergeSnapshots flattens multi-type Connected snapshots`() {
        val scope1 = testScope()
        val scope2 = testScope()
        val deviceApi1 = TestConnectedDeviceApi()
        val deviceApi2 = TestConnectedDeviceApi()

        val snapshots = listOf(
            ConnectionSnapshot(
                status = Connected(
                    scope1,
                    deviceApi1,
                    nonEmptyListOf(FInternalTransportConnectionType.LAN, FInternalTransportConnectionType.BLE)
                )
            ),
            ConnectionSnapshot(
                status = Connected(
                    scope2,
                    deviceApi2,
                    nonEmptyListOf(FInternalTransportConnectionType.CLOUD, FInternalTransportConnectionType.BLE)
                )
            )
        )

        val result = mergeSnapshots(snapshots)
        val resultStatus = assertIs<Connected>(result.status)

        assertEquals(
            listOf(
                FInternalTransportConnectionType.LAN,
                FInternalTransportConnectionType.BLE,
                FInternalTransportConnectionType.CLOUD
            ),
            resultStatus.connectionTypes.toList()
        )
    }

    // endregion

    // region mergeSnapshots - Connecting status

    @Test
    fun `mergeSnapshots combines connectionTypes from multiple Connecting snapshots`() {
        val snapshots = listOf(
            ConnectionSnapshot(
                status = Connecting(FInternalTransportConnectionType.LAN)
            ),
            ConnectionSnapshot(
                status = Connecting(FInternalTransportConnectionType.CLOUD)
            )
        )

        val result = mergeSnapshots(snapshots)
        val resultStatus = assertIs<Connecting>(result.status)

        assertEquals(
            listOf(FInternalTransportConnectionType.LAN, FInternalTransportConnectionType.CLOUD),
            resultStatus.connectionTypes.toList()
        )
    }

    @Test
    fun `mergeSnapshots deduplicates connectionTypes for Connecting`() {
        val snapshots = listOf(
            ConnectionSnapshot(
                status = Connecting(FInternalTransportConnectionType.BLE)
            ),
            ConnectionSnapshot(
                status = Connecting(FInternalTransportConnectionType.BLE)
            )
        )

        val result = mergeSnapshots(snapshots)
        val resultStatus = assertIs<Connecting>(result.status)

        assertEquals(
            listOf(FInternalTransportConnectionType.BLE),
            resultStatus.connectionTypes.toList()
        )
    }

    @Test
    fun `mergeSnapshots flattens multi-type Connecting snapshots`() {
        val snapshots = listOf(
            ConnectionSnapshot(
                status = Connecting(
                    nonEmptyListOf(FInternalTransportConnectionType.LAN, FInternalTransportConnectionType.BLE)
                )
            ),
            ConnectionSnapshot(
                status = Connecting(FInternalTransportConnectionType.CLOUD)
            )
        )

        val result = mergeSnapshots(snapshots)
        val resultStatus = assertIs<Connecting>(result.status)

        assertEquals(
            listOf(
                FInternalTransportConnectionType.LAN,
                FInternalTransportConnectionType.BLE,
                FInternalTransportConnectionType.CLOUD
            ),
            resultStatus.connectionTypes.toList()
        )
    }

    // endregion

    // region mergeSnapshots - statuses without connectionType

    @Test
    fun `mergeSnapshots with multiple Disconnected returns Disconnected`() {
        val snapshots = listOf(
            ConnectionSnapshot(status = Disconnected()),
            ConnectionSnapshot(status = Disconnected())
        )

        val result = mergeSnapshots(snapshots)

        assertEquals(Disconnected(), result.status)
    }

    @Test
    fun `mergeSnapshots with multiple Disconnecting returns Disconnecting`() {
        val snapshots = listOf(
            ConnectionSnapshot(status = Disconnecting),
            ConnectionSnapshot(status = Disconnecting)
        )

        val result = mergeSnapshots(snapshots)

        assertEquals(Disconnecting, result.status)
    }

    // endregion

    // region mergeSnapshots - capabilities

    @Test
    fun `mergeSnapshots combines capabilities from multiple snapshots`() {
        val snapshots = listOf(
            ConnectionSnapshot(
                capabilities = listOf(FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED),
                status = Disconnected()
            ),
            ConnectionSnapshot(
                capabilities = listOf(FHTTPTransportCapability.BB_LOCAL_CONNECTION),
                status = Disconnected()
            )
        )

        val result = mergeSnapshots(snapshots)

        assertEquals(
            listOf(
                FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED,
                FHTTPTransportCapability.BB_LOCAL_CONNECTION
            ),
            result.capabilities
        )
    }

    @Test
    fun `mergeSnapshots deduplicates capabilities`() {
        val snapshots = listOf(
            ConnectionSnapshot(
                capabilities = listOf(
                    FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED,
                    FHTTPTransportCapability.BB_LOCAL_CONNECTION
                ),
                status = Disconnected()
            ),
            ConnectionSnapshot(
                capabilities = listOf(
                    FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED,
                    FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED
                ),
                status = Disconnected()
            )
        )

        val result = mergeSnapshots(snapshots)

        assertEquals(
            listOf(
                FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED,
                FHTTPTransportCapability.BB_LOCAL_CONNECTION,
                FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED
            ),
            result.capabilities
        )
    }

    @Test
    fun `mergeSnapshots returns null capabilities when all snapshots have null capabilities`() {
        val snapshots = listOf(
            ConnectionSnapshot(capabilities = null, status = Disconnected()),
            ConnectionSnapshot(capabilities = null, status = Disconnected())
        )

        val result = mergeSnapshots(snapshots)

        assertNull(result.capabilities)
    }

    @Test
    fun `mergeSnapshots combines capabilities when some are null and some are not`() {
        val snapshots = listOf(
            ConnectionSnapshot(
                capabilities = listOf(FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED),
                status = Disconnected()
            ),
            ConnectionSnapshot(
                capabilities = null,
                status = Disconnected()
            )
        )

        val result = mergeSnapshots(snapshots)

        assertEquals(
            listOf(FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED),
            result.capabilities
        )
    }

    @Test
    fun `mergeSnapshots combines capabilities and connectionTypes together for Connected`() {
        val scope1 = testScope()
        val scope2 = testScope()
        val deviceApi1 = TestConnectedDeviceApi()
        val deviceApi2 = TestConnectedDeviceApi()

        val snapshots = listOf(
            ConnectionSnapshot(
                capabilities = listOf(FHTTPTransportCapability.BB_LOCAL_CONNECTION),
                status = Connected(scope1, deviceApi1, FInternalTransportConnectionType.LAN)
            ),
            ConnectionSnapshot(
                capabilities = listOf(FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED),
                status = Connected(scope2, deviceApi2, FInternalTransportConnectionType.CLOUD)
            )
        )

        val result = mergeSnapshots(snapshots)

        assertEquals(
            listOf(
                FHTTPTransportCapability.BB_LOCAL_CONNECTION,
                FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED
            ),
            result.capabilities
        )
        val resultStatus = assertIs<Connected>(result.status)
        assertEquals(
            listOf(FInternalTransportConnectionType.LAN, FInternalTransportConnectionType.CLOUD),
            resultStatus.connectionTypes.toList()
        )
    }

    // endregion

    // region getPriority

    @Test
    fun `getPriority returns correct ordering`() {
        val scope = testScope()
        val deviceApi = TestConnectedDeviceApi()

        val disconnected = getPriority(Disconnected())
        val connecting = getPriority(Connecting(FInternalTransportConnectionType.BLE))
        val disconnecting = getPriority(Disconnecting)
        val connected = getPriority(Connected(scope, deviceApi, FInternalTransportConnectionType.LAN))

        assertTrue(disconnected < connecting, "Disconnected < Connecting")
        assertTrue(connecting < disconnecting, "Connecting < Disconnecting")
        assertTrue(disconnecting < connected, "Disconnecting < Connected")
    }

    // endregion
}
