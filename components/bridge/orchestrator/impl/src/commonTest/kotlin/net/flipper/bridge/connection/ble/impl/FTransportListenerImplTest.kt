package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FTransportListenerImplTest {
    @Test
    fun GIVEN_default_state_WHEN_connecting_THEN_emits_connecting() = runTest {
        val device = busyBar()
        val listener = FTransportListenerImpl(device)

        listener.onStatusUpdate(
            device,
            FInternalTransportConnectionStatus.Connecting(FInternalTransportConnectionType.BLE)
        )

        val state = assertIs<FDeviceConnectStatus.Connecting.InProgress>(
            listener.getState().value
        )
        assertEquals(device, state.device)
    }

    @Test
    fun GIVEN_recoverable_disconnect_WHEN_retry_connecting_THEN_emits_connecting() = runTest {
        val device = busyBar()
        val listener = FTransportListenerImpl(device)

        listener.onStatusUpdate(
            device,
            FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.OTHER)
        )
        listener.onStatusUpdate(
            device,
            FInternalTransportConnectionStatus.Connecting(FInternalTransportConnectionType.BLE)
        )

        val state = assertIs<FDeviceConnectStatus.Connecting.InProgress>(listener.getState().value)
        assertEquals(device, state.device)
    }

    @Test
    fun GIVEN_recoverable_disconnect_WHEN_retry_connects_THEN_emits_connected() = runTest {
        val device = busyBar()
        val listener = FTransportListenerImpl(device)
        val deviceApi = TestConnectedDeviceApi()

        listener.onStatusUpdate(
            device,
            FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.OTHER)
        )
        listener.onStatusUpdate(
            device,
            FInternalTransportConnectionStatus.Connected(
                scope = this,
                deviceApi = deviceApi,
                connectionType = FInternalTransportConnectionType.BLE
            )
        )

        val state = assertIs<FDeviceConnectStatus.Connected>(listener.getState().value)
        assertEquals(device, state.device)
        assertEquals(deviceApi, state.deviceApi)
    }

    @Test
    fun GIVEN_recoverable_disconnect_WHEN_requires_repairing_THEN_replaces_disconnected() = runTest {
        val device = busyBar()
        val listener = FTransportListenerImpl(device)

        listener.onStatusUpdate(
            device,
            FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.OTHER)
        )
        listener.onStatusUpdate(
            device,
            FInternalTransportConnectionStatus.Disconnected(
                FInternalDisconnectedReason.REQUIRES_REPAIRING
            )
        )

        val state = assertIs<FDeviceConnectStatus.Disconnected>(listener.getState().value)
        assertEquals(device, state.device)
        assertEquals(DisconnectStatus.REQUIRES_REPAIRING, state.reason)
    }

    private fun busyBar(uniqueId: String = "device-id"): BUSYBar {
        return BUSYBar(
            humanReadableName = "Device",
            uniqueId = uniqueId,
            ble = BUSYBar.ConnectionWay.BLE("AA:BB:CC:DD:EE:FF")
        )
    }

    private class TestConnectedDeviceApi : FConnectedDeviceApi {
        override val deviceName = "Device"

        override suspend fun tryUpdateConnectionConfig(
            config: FDeviceConnectionConfig<*>
        ): Result<Unit> = Result.success(Unit)

        override suspend fun disconnect() = Unit
    }
}
