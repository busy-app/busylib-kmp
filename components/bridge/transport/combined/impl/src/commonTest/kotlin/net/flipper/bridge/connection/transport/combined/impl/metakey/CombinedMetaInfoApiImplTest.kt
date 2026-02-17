package net.flipper.bridge.connection.transport.combined.impl.metakey

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.MockConnectionBuilder
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.helpers.TestConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CombinedMetaInfoApiImplTest {

    @Test
    fun GIVEN_no_connected_transports_WHEN_get_called_THEN_emits_failure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val connectionBuilder = MockConnectionBuilder()
        val connection = AutoReconnectConnection(
            scope = backgroundScope,
            config = TestConfig(),
            connectionBuilder = connectionBuilder,
            dispatcher = testDispatcher
        )

        connectionBuilder.connectCalledDeferred.await()
        advanceUntilIdle()

        val sut = CombinedMetaInfoApiImpl(listOf(connection))
        val result = sut.get(TransportMetaInfoKey.DEVICE_NAME).first()

        assertTrue(result.isFailure, "Should be failure when no transport is connected")

        connection.disconnect()
        advanceUntilIdle()
    }

    @Test
    fun GIVEN_connected_transport_supports_key_WHEN_get_called_THEN_emits_success_with_data() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val connectionBuilder = MockConnectionBuilder()
            val connection = AutoReconnectConnection(
                scope = backgroundScope,
                config = TestConfig(),
                connectionBuilder = connectionBuilder,
                dispatcher = testDispatcher
            )

            connectionBuilder.connectCalledDeferred.await()
            advanceUntilIdle()

            val expectedData = TransportMetaInfoData.RawBytes("TestDevice".encodeToByteArray())
            val metaDeviceApi = TestMetaInfoDeviceApi(
                supportedKeys = mapOf(
                    TransportMetaInfoKey.DEVICE_NAME to flowOf(expectedData)
                )
            )
            connectionBuilder.latestListener()!!.onStatusUpdate(
                FInternalTransportConnectionStatus.Connected(
                    scope = backgroundScope,
                    deviceApi = metaDeviceApi
                )
            )
            advanceUntilIdle()

            val sut = CombinedMetaInfoApiImpl(listOf(connection))
            val result = sut.get(TransportMetaInfoKey.DEVICE_NAME).first()

            assertTrue(result.isSuccess, "Should be success when transport supports the key")
            val dataFlow = result.getOrThrow()
            assertEquals(expectedData, dataFlow.first())

            connection.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_connected_transport_does_not_support_key_WHEN_get_called_THEN_emits_failure() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val connectionBuilder = MockConnectionBuilder()
            val connection = AutoReconnectConnection(
                scope = backgroundScope,
                config = TestConfig(),
                connectionBuilder = connectionBuilder,
                dispatcher = testDispatcher
            )

            connectionBuilder.connectCalledDeferred.await()
            advanceUntilIdle()

            val metaDeviceApi = TestMetaInfoDeviceApi(
                supportedKeys = mapOf(
                    TransportMetaInfoKey.DEVICE_NAME
                        to flowOf(TransportMetaInfoData.RawBytes("Test".encodeToByteArray()))
                )
            )
            connectionBuilder.latestListener()!!.onStatusUpdate(
                FInternalTransportConnectionStatus.Connected(
                    scope = backgroundScope,
                    deviceApi = metaDeviceApi
                )
            )
            advanceUntilIdle()

            val sut = CombinedMetaInfoApiImpl(listOf(connection))
            val result = sut.get(TransportMetaInfoKey.BATTERY_LEVEL).first()

            assertTrue(
                result.isFailure,
                "Should be failure when transport does not support the key"
            )

            connection.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_connected_device_without_meta_api_WHEN_get_called_THEN_emits_failure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val connectionBuilder = MockConnectionBuilder()
        val connection = AutoReconnectConnection(
            scope = backgroundScope,
            config = TestConfig(),
            connectionBuilder = connectionBuilder,
            dispatcher = testDispatcher
        )

        connectionBuilder.connectCalledDeferred.await()
        advanceUntilIdle()

        val plainDeviceApi = TestConnectedDeviceApi()
        connectionBuilder.latestListener()!!.onStatusUpdate(
            FInternalTransportConnectionStatus.Connected(
                scope = backgroundScope,
                deviceApi = plainDeviceApi
            )
        )
        advanceUntilIdle()

        val sut = CombinedMetaInfoApiImpl(listOf(connection))
        val result = sut.get(TransportMetaInfoKey.DEVICE_NAME).first()

        assertTrue(
            result.isFailure,
            "Should be failure when device does not implement FTransportMetaInfoApi"
        )

        connection.disconnect()
        advanceUntilIdle()
    }

    @Test
    fun GIVEN_two_connections_only_second_supports_key_WHEN_get_called_THEN_returns_from_second() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)

            val builder1 = MockConnectionBuilder()
            val connection1 = AutoReconnectConnection(
                scope = backgroundScope,
                config = TestConfig(),
                connectionBuilder = builder1,
                dispatcher = testDispatcher
            )
            builder1.connectCalledDeferred.await()
            advanceUntilIdle()

            val builder2 = MockConnectionBuilder()
            val connection2 = AutoReconnectConnection(
                scope = backgroundScope,
                config = TestConfig(),
                connectionBuilder = builder2,
                dispatcher = testDispatcher
            )
            builder2.connectCalledDeferred.await()
            advanceUntilIdle()

            val meta1 = TestMetaInfoDeviceApi(
                supportedKeys = mapOf(
                    TransportMetaInfoKey.DEVICE_NAME to flowOf(
                        TransportMetaInfoData.RawBytes("Device1".encodeToByteArray())
                    )
                )
            )
            builder1.latestListener()!!.onStatusUpdate(
                FInternalTransportConnectionStatus.Connected(
                    scope = backgroundScope,
                    deviceApi = meta1
                )
            )

            val expectedBattery = TransportMetaInfoData.RawBytes(byteArrayOf(50))
            val meta2 = TestMetaInfoDeviceApi(
                supportedKeys = mapOf(
                    TransportMetaInfoKey.BATTERY_LEVEL to flowOf(expectedBattery)
                )
            )
            builder2.latestListener()!!.onStatusUpdate(
                FInternalTransportConnectionStatus.Connected(
                    scope = backgroundScope,
                    deviceApi = meta2
                )
            )
            advanceUntilIdle()

            val sut = CombinedMetaInfoApiImpl(listOf(connection1, connection2))
            val result = sut.get(TransportMetaInfoKey.BATTERY_LEVEL).first()

            assertTrue(result.isSuccess, "Should find key from second connection")
            assertEquals(expectedBattery, result.getOrThrow().first())

            connection1.disconnect()
            connection2.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_transport_disconnects_WHEN_observing_key_THEN_emits_failure_after_disconnect() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val connectionBuilder = MockConnectionBuilder()
            val connection = AutoReconnectConnection(
                scope = backgroundScope,
                config = TestConfig(),
                connectionBuilder = connectionBuilder,
                dispatcher = testDispatcher
            )

            connectionBuilder.connectCalledDeferred.await()
            advanceUntilIdle()

            val metaDeviceApi = TestMetaInfoDeviceApi(
                supportedKeys = mapOf(
                    TransportMetaInfoKey.DEVICE_NAME to flowOf(
                        TransportMetaInfoData.RawBytes("Test".encodeToByteArray())
                    )
                )
            )
            val listener = connectionBuilder.latestListener()!!
            listener.onStatusUpdate(
                FInternalTransportConnectionStatus.Connected(
                    scope = backgroundScope,
                    deviceApi = metaDeviceApi
                )
            )
            advanceUntilIdle()

            val sut = CombinedMetaInfoApiImpl(listOf(connection))

            // Verify initially successful
            val resultBefore = sut.get(TransportMetaInfoKey.DEVICE_NAME).first()
            assertTrue(resultBefore.isSuccess, "Should initially succeed")

            // Disconnect
            listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
            advanceUntilIdle()

            // After disconnect, should emit failure
            val resultAfter = sut.get(TransportMetaInfoKey.DEVICE_NAME).first()
            assertTrue(resultAfter.isFailure, "Should fail after disconnect")

            connection.disconnect()
            advanceUntilIdle()
        }

    @Test
    fun GIVEN_transport_reconnects_WHEN_was_disconnected_THEN_emits_success_again() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val connectionBuilder = MockConnectionBuilder()
        val connection = AutoReconnectConnection(
            scope = backgroundScope,
            config = TestConfig(),
            connectionBuilder = connectionBuilder,
            dispatcher = testDispatcher
        )

        connectionBuilder.connectCalledDeferred.await()
        advanceUntilIdle()

        val listener = connectionBuilder.latestListener()!!

        val sut = CombinedMetaInfoApiImpl(listOf(connection))

        // Initially no connection — failure
        val result1 = sut.get(TransportMetaInfoKey.DEVICE_NAME).first()
        assertTrue(result1.isFailure, "Should fail when not connected")

        // Connect
        val expectedData = TransportMetaInfoData.RawBytes("Reconnected".encodeToByteArray())
        val metaDeviceApi = TestMetaInfoDeviceApi(
            supportedKeys = mapOf(
                TransportMetaInfoKey.DEVICE_NAME to flowOf(expectedData)
            )
        )
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Connected(
                scope = backgroundScope,
                deviceApi = metaDeviceApi
            )
        )
        advanceUntilIdle()

        // Now should succeed
        val result2 = sut.get(TransportMetaInfoKey.DEVICE_NAME).first()
        assertTrue(result2.isSuccess, "Should succeed after reconnect")
        assertEquals(expectedData, result2.getOrThrow().first())

        connection.disconnect()
        advanceUntilIdle()
    }
}

/**
 * Test device API that implements both [FConnectedDeviceApi] and [FTransportMetaInfoApi].
 */
private class TestMetaInfoDeviceApi(
    override val deviceName: String = "TestMetaDevice",
    private val supportedKeys: Map<TransportMetaInfoKey, Flow<TransportMetaInfoData?>> = emptyMap()
) : FConnectedDeviceApi, FTransportMetaInfoApi {

    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>> {
        val flow = supportedKeys[key]
            ?: return flowOf(Result.failure(NoSuchElementException("Key $key not supported")))
        return flowOf(Result.success(flow))
    }

    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun disconnect() = Unit
}
