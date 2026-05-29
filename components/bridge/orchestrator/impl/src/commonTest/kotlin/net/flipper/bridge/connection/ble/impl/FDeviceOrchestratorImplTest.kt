package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.core.busylib.data.nonEmptyListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class FDeviceOrchestratorImplTest {
    @Test
    fun GIVEN_same_device_recoverable_disconnect_WHEN_connect_if_not_THEN_reuses_holder() = runTest {
        val device = busyBar()
        val config = TestConnectionConfig()
        val deviceApi = TestConnectedDeviceApi()
        val connectionHelper = RecordingConnectionHelper(deviceApi)
        val sut = FDeviceOrchestratorImpl(
            deviceHolderFactory = FDeviceHolderFactory(connectionHelper),
            deviceConnectionConfigMapper = TestConfigMapper(config),
            globalScope = backgroundScope
        )

        try {
            sut.connectIfNot(device)
            val listener = connectionHelper.awaitListener()
            connectionHelper.awaitConnectCalls(1)
            delay(CONNECT_COMPLETION_DELAY_MS)

            listener.onStatusUpdate(
                FInternalTransportConnectionStatus.Disconnected(FInternalDisconnectedReason.OTHER)
            )
            runCurrent()
            sut.connectIfNot(device)

            assertEquals(1, connectionHelper.connectCalls.value)
            assertEquals(1, deviceApi.tryUpdateCalls.value)
        } finally {
            sut.disconnectCurrent()
        }
    }

    private fun busyBar(uniqueId: String = "device-id"): BUSYBar {
        return BUSYBar(
            humanReadableName = "Device",
            uniqueId = uniqueId,
            ble = BUSYBar.ConnectionWay.BLE("AA:BB:CC:DD:EE:FF")
        )
    }

    private class TestConfigMapper(
        private val config: FDeviceConnectionConfig<*>
    ) : FDeviceConnectionConfigMapper {
        override fun getConnectionConfig(device: BUSYBar): FDeviceConnectionConfig<*> = config
    }

    private class RecordingConnectionHelper(
        private val deviceApi: TestConnectedDeviceApi
    ) : FDeviceConfigToConnection {
        val connectCalls = MutableStateFlow(0)
        private val listenerDeferred = CompletableDeferred<FTransportConnectionStatusListener>()

        override suspend fun <API : FConnectedDeviceApi, CONFIG : FDeviceConnectionConfig<API>> connect(
            scope: CoroutineScope,
            config: CONFIG,
            listener: FTransportConnectionStatusListener
        ): Result<API> {
            connectCalls.update { it + 1 }
            listenerDeferred.complete(listener)
            @Suppress("UNCHECKED_CAST")
            return Result.success(deviceApi as API)
        }

        suspend fun awaitListener(): FTransportConnectionStatusListener {
            return withTimeout(AWAIT_TIMEOUT_MS) { listenerDeferred.await() }
        }

        suspend fun awaitConnectCalls(expected: Int) {
            withTimeout(AWAIT_TIMEOUT_MS) {
                connectCalls.first { it == expected }
            }
        }
    }

    private class TestConnectionConfig : FDeviceConnectionConfig<TestConnectedDeviceApi>() {
        override fun getTransportTypes() = nonEmptyListOf(FInternalTransportConnectionType.BLE)
    }

    private class TestConnectedDeviceApi : FConnectedDeviceApi {
        val tryUpdateCalls = MutableStateFlow(0)

        override val deviceName = "Device"

        override suspend fun tryUpdateConnectionConfig(
            config: FDeviceConnectionConfig<*>
        ): Result<Unit> {
            tryUpdateCalls.update { it + 1 }
            return Result.success(Unit)
        }

        override suspend fun disconnect() = Unit
    }

    private companion object {
        const val AWAIT_TIMEOUT_MS = 5_000L
        const val CONNECT_COMPLETION_DELAY_MS = 100L
    }
}
