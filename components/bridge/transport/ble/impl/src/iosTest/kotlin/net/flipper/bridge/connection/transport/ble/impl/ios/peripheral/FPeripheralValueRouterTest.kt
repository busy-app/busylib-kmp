package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.DEVICE_NAME_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_RX_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.error
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.newCharacteristic
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FPeripheralValueRouterTest {

    @Test
    fun GIVEN_serial_rx_characteristic_WHEN_didUpdateValue_called_THEN_data_is_emitted_to_rx_stream() = runTest {
        val sut = createSut().sut
        val payload = byteArrayOf(9, 8, 7)
        val rxCharacteristic = newCharacteristic(SERIAL_RX_SHORT_UUID, payload = payload)

        val rxDeferred = async { sut.rxDataStream.first() }
        runCurrent()

        sut.didUpdateValue(rxCharacteristic, error = null)

        assertContentEquals(payload, rxDeferred.await())
    }

    @Test
    fun GIVEN_multiple_rx_chunks_WHEN_didUpdateValue_called_rapidly_THEN_all_received_in_order() = runTest {
        val sut = createSut().sut
        val chunks = List(50) { i -> byteArrayOf(i.toByte(), (i * 2).toByte()) }

        chunks.forEach { payload ->
            sut.didUpdateValue(
                newCharacteristic(SERIAL_RX_SHORT_UUID, payload = payload),
                error = null,
            )
        }

        val received = sut.rxDataStream.take(chunks.size).toList()

        assertEquals(chunks.size, received.size)
        chunks.forEachIndexed { index, expected ->
            assertContentEquals(expected, received[index])
        }
    }

    @Test
    fun GIVEN_no_active_collector_WHEN_rx_chunks_arrive_THEN_data_buffered_and_available_later() = runTest {
        val sut = createSut().sut
        val chunks = List(5) { i -> byteArrayOf(i.toByte()) }

        // Send without any active collector — data must buffer in channel
        chunks.forEach { payload ->
            sut.didUpdateValue(
                newCharacteristic(SERIAL_RX_SHORT_UUID, payload = payload),
                error = null,
            )
        }

        // Start collecting after all sends — all buffered data should be available
        val received = sut.rxDataStream.take(chunks.size).toList()

        assertEquals(chunks.size, received.size)
        chunks.forEachIndexed { index, expected ->
            assertContentEquals(expected, received[index])
        }
    }

    @Test
    fun GIVEN_screen_streaming_burst_WHEN_many_chunks_arrive_THEN_no_data_lost_and_order_preserved() = runTest {
        val sut = createSut().sut
        // Realistic BLE chunk size (128 bytes), ~3 frames × 20 chunks each
        val totalChunks = 60
        val chunks = List(totalChunks) { i ->
            ByteArray(128) { byteIndex -> (i + byteIndex).toByte() }
        }

        chunks.forEach { payload ->
            sut.didUpdateValue(
                newCharacteristic(SERIAL_RX_SHORT_UUID, payload = payload),
                error = null,
            )
        }

        val received = sut.rxDataStream.take(totalChunks).toList()

        assertEquals(totalChunks, received.size)
        chunks.forEachIndexed { index, expected ->
            assertContentEquals(expected, received[index])
        }
    }

    @Test
    fun GIVEN_rx_stream_collected_WHEN_disconnect_closes_channel_THEN_flow_completes() = runTest {
        val sut = createConnectedSut()
        val payload = byteArrayOf(42)

        val collectJob = async {
            sut.sut.rxDataStream.toList()
        }
        runCurrent()

        sut.sut.didUpdateValue(
            newCharacteristic(SERIAL_RX_SHORT_UUID, payload = payload),
            error = null,
        )
        // BLEEventQueue forwards the chunk asynchronously — drain it before
        // closing the channel, otherwise the send races with onDisconnect.
        runCurrent()
        sut.sut.onDisconnect()
        runCurrent()

        val received = collectJob.await()

        assertEquals(1, received.size)
        assertContentEquals(payload, received[0])
    }

    @Test
    fun GIVEN_disconnect_already_happened_WHEN_collecting_rx_stream_THEN_flow_completes_immediately_empty() = runTest {
        val sut = createConnectedSut()

        sut.sut.onDisconnect()
        runCurrent()

        val received = sut.sut.rxDataStream.toList()

        assertTrue(received.isEmpty())
    }

    @Test
    fun GIVEN_meta_characteristic_update_WHEN_didUpdateValue_called_THEN_meta_updates_and_state_connected() = runTest {
        val sut = createSut().sut
        val payload = "BusyBar".encodeToByteArray()
        val metaCharacteristic = newCharacteristic(DEVICE_NAME_SHORT_UUID, payload = payload)

        sut.didUpdateValue(metaCharacteristic, error = null)
        // BLEEventQueue processes the update asynchronously — wait for it.
        sut.stateStream.first { it == FPeripheralState.CONNECTED }
        sut.metaInfoKeysStream.first { it.isNotEmpty() }

        assertEquals(FPeripheralState.CONNECTED, sut.stateStream.value)
        assertContentEquals(payload, sut.metaInfoKeysStream.value.getValue(TransportMetaInfoKey.DEVICE_NAME))
    }

    @Test
    fun GIVEN_meta_update_error_WHEN_didUpdateValue_called_THEN_error_is_propagated_to_state_machine() = runTest {
        val sut = createSut().sut
        val metaCharacteristic = newCharacteristic(DEVICE_NAME_SHORT_UUID)

        sut.didUpdateValue(
            characteristic = metaCharacteristic,
            error = error(domain = "CBATTErrorDomain", code = 15L),
        )

        // onError schedules state updates via scope.launch, so await the transition
        sut.stateStream.first { it == FPeripheralState.PAIRING_FAILED }
    }

    @Test
    fun GIVEN_unknown_non_meta_characteristic_WHEN_didUpdateValue_called_THEN_state_and_meta_remain_unchanged() =
        runTest {
            val sut = createSut().sut
            val payload = byteArrayOf(7, 7, 7)

            sut.didUpdateValue(newCharacteristic("2A99", payload = payload), error = null)
            runCurrent()

            assertEquals(FPeripheralState.DISCONNECTED, sut.stateStream.value)
            assertEquals(emptyMap(), sut.metaInfoKeysStream.value)
        }
}
