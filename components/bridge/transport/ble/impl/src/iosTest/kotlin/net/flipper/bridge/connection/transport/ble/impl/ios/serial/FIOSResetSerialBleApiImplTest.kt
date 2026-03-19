package net.flipper.bridge.connection.transport.ble.impl.ios.serial

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralApi
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralState
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.createConfig
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class FIOSResetSerialBleApiImplTest {
    @Test
    fun GIVEN_polled_reset_characteristic_WHEN_payload_is_little_endian_uint32_THEN_stateflow_emits_counter() =
        runTest {
            val peripheral = FakePeripheralApi()
            val config = createConfig(macAddress = peripheral.identifier.UUIDString)
            peripheral.enqueueReadValue(byteArrayOf(0x3, 0x0, 0x0, 0x0))

            val sut = FIOSResetSerialBleApiImpl(
                scope = backgroundScope,
                fPeripheralApi = peripheral,
                config = config,
            )

            assertEquals(3, sut.getRequestCounterStateFlow().first { it == 3 })
            assertEquals(listOf(config.serialConfig.resetCharUuid), peripheral.readRequests)
        }

    @Test
    fun GIVEN_multiple_calls_to_getRequestCounterStateFlow_WHEN_called_THEN_same_stable_flow_is_returned() =
        runTest {
            val peripheral = FakePeripheralApi()
            val config = createConfig(macAddress = peripheral.identifier.UUIDString)
            val sut = FIOSResetSerialBleApiImpl(
                scope = backgroundScope,
                fPeripheralApi = peripheral,
                config = config,
            )

            val firstFlow = sut.getRequestCounterStateFlow()
            val secondFlow = sut.getRequestCounterStateFlow()

            assertSame(firstFlow, secondFlow)
        }

    @Test
    fun GIVEN_reset_called_WHEN_write_succeeds_and_polled_value_becomes_zero_THEN_reset_completes() =
        runTest {
            val peripheral = FakePeripheralApi()
            val config = createConfig(macAddress = peripheral.identifier.UUIDString)
            peripheral.enqueueReadValue(byteArrayOf(0x3, 0x0, 0x0, 0x0))
            peripheral.enqueueReadValue(byteArrayOf(0x5, 0x0, 0x0, 0x0))
            peripheral.enqueueReadValue(byteArrayOf(0x0, 0x0, 0x0, 0x0))

            val sut = FIOSResetSerialBleApiImpl(
                scope = backgroundScope,
                fPeripheralApi = peripheral,
                config = config,
            )

            assertEquals(3, sut.getRequestCounterStateFlow().first { it == 3 })

            val resetJob = async { sut.reset() }
            runCurrent()
            assertEquals(1, peripheral.writeRequests.size)
            assertEquals(config.serialConfig.resetCharUuid, peripheral.writeRequests.single().first)
            assertContentEquals(byteArrayOf(0, 0, 0, 0), peripheral.writeRequests.single().second)
            assertFalse(resetJob.isCompleted)

            advanceTimeBy(5.seconds.inWholeMilliseconds)
            runCurrent()
            resetJob.await()
        }

    @Test
    fun GIVEN_reset_called_WHEN_characteristic_missing_or_read_fails_THEN_exception_is_propagated() =
        runTest {
            val peripheral = FakePeripheralApi(
                writeFailure = IllegalStateException("Reset characteristic not found")
            )
            val config = createConfig(macAddress = peripheral.identifier.UUIDString)
            val sut = FIOSResetSerialBleApiImpl(
                scope = backgroundScope,
                fPeripheralApi = peripheral,
                config = config,
            )

            assertFailsWith<IllegalStateException> {
                sut.reset()
            }
        }
}

private class FakePeripheralApi(
    private val writeFailure: Throwable? = null,
) : FPeripheralApi {
    override val identifier: NSUUID = NSUUID()
    override val name: String? = "FakePeripheral"
    override val stateStream: WrappedStateFlow<FPeripheralState> = MutableStateFlow(FPeripheralState.CONNECTED)
        .asStateFlow()
        .wrap()
    override val rxDataStream: Flow<ByteArray> = emptyFlow()
    override val metaInfoKeysStream: WrappedStateFlow<Map<TransportMetaInfoKey, ByteArray?>> =
        MutableStateFlow<Map<TransportMetaInfoKey, ByteArray?>>(emptyMap())
            .asStateFlow()
            .wrap()

    val readRequests = mutableListOf<Uuid>()
    val writeRequests = mutableListOf<Pair<Uuid, ByteArray>>()
    private val readQueue = ArrayDeque<Result<ByteArray?>>()
    private var fallbackReadValue: ByteArray? = byteArrayOf(0, 0, 0, 0)
    private val characteristicValueFlow = MutableStateFlow<ByteArray?>(null)

    fun enqueueReadValue(value: ByteArray?) {
        readQueue.addLast(Result.success(value))
    }

    override suspend fun writeValue(data: ByteArray) = Unit

    override suspend fun readValue(characteristicUuid: Uuid): ByteArray {
        readRequests += characteristicUuid
        val result = readQueue.removeFirstOrNull()
            ?: return fallbackReadValue ?: error("No read value available")
        val value = result.getOrThrow()
        if (value != null) {
            fallbackReadValue = value
        }
        return value ?: error("No read value available")
    }

    override suspend fun writeValue(characteristicUuid: Uuid, data: ByteArray) {
        writeFailure?.let { throw it }
        writeRequests += characteristicUuid to data
    }

    override suspend fun onConnecting() = Unit
    override suspend fun onConnect() = Unit
    override suspend fun onDisconnecting() = Unit
    override suspend fun onDisconnect() = Unit
    override fun onError(error: NSError) = Unit
}
