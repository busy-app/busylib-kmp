package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.core.busylib.data.nonEmptyListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FTransportListenerImplTest {
    @Test
    fun GIVEN_default_state_WHEN_connecting_THEN_emits_connecting() = runTest {
        val context = listenerContext()

        context.listener.onStatusUpdate(
            context.holder,
            FInternalTransportConnectionStatus.Connecting(FInternalTransportConnectionType.BLE)
        )

        val state = assertIs<FDeviceConnectStatus.Connecting.InProgress>(
            context.listener.getState().value
        )
        assertEquals(context.device, state.device)
    }

    @Test
    fun GIVEN_recoverable_disconnect_WHEN_retry_connecting_THEN_emits_connecting() = runTest {
        val context = listenerContext()

        context.listener.onStatusUpdate(
            context.holder,
            FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.OTHER)
        )
        context.listener.onStatusUpdate(
            context.holder,
            FInternalTransportConnectionStatus.Connecting(FInternalTransportConnectionType.BLE)
        )

        val state = assertIs<FDeviceConnectStatus.Connecting.InProgress>(
            context.listener.getState().value
        )
        assertEquals(context.device, state.device)
    }

    @Test
    fun GIVEN_recoverable_disconnect_WHEN_retry_connects_THEN_emits_connected() = runTest {
        val context = listenerContext()
        val deviceApi = TestConnectedDeviceApi()

        context.listener.onStatusUpdate(
            context.holder,
            FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.OTHER)
        )
        context.listener.onStatusUpdate(
            context.holder,
            FInternalTransportConnectionStatus.Connected(
                scope = this,
                deviceApi = deviceApi,
                connectionType = FInternalTransportConnectionType.BLE
            )
        )

        val state = assertIs<FDeviceConnectStatus.Connected>(context.listener.getState().value)
        assertEquals(context.device, state.device)
        assertEquals(deviceApi, state.deviceApi)
    }

    @Test
    fun GIVEN_recoverable_disconnect_WHEN_requires_repairing_THEN_replaces_disconnected() = runTest {
        val context = listenerContext()

        context.listener.onStatusUpdate(
            context.holder,
            FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.OTHER)
        )
        context.listener.onStatusUpdate(
            context.holder,
            FInternalTransportConnectionStatus.Disconnected(
                FInternalDisconnectedReason.REQUIRES_REPAIRING
            )
        )

        val state = assertIs<FDeviceConnectStatus.Disconnected>(context.listener.getState().value)
        assertEquals(context.device, state.device)
        assertEquals(DisconnectStatus.REQUIRES_REPAIRING, state.reason)
    }

    private fun listenerContext(device: BUSYBar = busyBar()): ListenerContext {
        val listener = FTransportListenerImpl(device) { _, postAction ->
            postAction()
        }
        val holder = FDeviceHolder(
            uniqueId = device.uniqueId,
            config = TestDeviceConnectionConfig(),
            listener = listener,
            deviceConnectionHelper = TestDeviceConfigToConnection(TestConnectedDeviceApi())
        )
        return ListenerContext(
            device = device,
            listener = listener,
            holder = holder
        )
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

    private data class ListenerContext(
        val device: BUSYBar,
        val listener: FTransportListenerImpl,
        val holder: FDeviceHolder<TestConnectedDeviceApi>
    )

    private class TestDeviceConnectionConfig : FDeviceConnectionConfig<TestConnectedDeviceApi>() {
        override fun getTransportTypes() = nonEmptyListOf(FInternalTransportConnectionType.MOCK)
    }

    private class TestDeviceConfigToConnection(
        private val deviceApi: TestConnectedDeviceApi
    ) : FDeviceConfigToConnection {
        @Suppress("UNCHECKED_CAST")
        override suspend fun <API : FConnectedDeviceApi, CONFIG : FDeviceConnectionConfig<API>> connect(
            scope: CoroutineScope,
            config: CONFIG,
            listener: FTransportConnectionStatusListener
        ): Result<API> {
            return Result.success(deviceApi as API)
        }
    }
}
