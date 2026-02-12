package net.flipper.transport.ble.impl.cb

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.api.MAX_ATTRIBUTE_SIZE
import net.flipper.bridge.connection.transport.ble.api.WRITE_ACK_TIMEOUT_MS
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FPeripheralTest {

    @Test
    fun GIVEN_initial_peripheral_state_WHEN_creating_fperipheral_THEN_stateStream_is_initialized_from_cb_state() =
        runTest {
            val recordingPeripheral = RecordingPeripheral().apply { setStateRaw(2L) }
            val sut = FPeripheral(recordingPeripheral, createConfig(recordingPeripheral.identifier.UUIDString), this)

            assertEquals(FPeripheralState.CONNECTED, sut.stateStream.value)
        }

    @Test
    fun GIVEN_on_connecting_and_disconnecting_WHEN_callbacks_invoked_THEN_stateStream_emits_transitions() = runTest {
        val sut = createSut().sut

        sut.onConnecting()
        assertEquals(FPeripheralState.CONNECTING, sut.stateStream.value)

        sut.onDisconnecting()
        assertEquals(FPeripheralState.DISCONNECTING, sut.stateStream.value)
    }

    @Test
    fun GIVEN_on_connect_WHEN_invoked_THEN_service_discovery_starts() = runTest {
        val sut = createSut()

        sut.sut.onConnect()

        assertEquals(1, sut.peripheral.discoverServicesCalls)
    }

    @Test
    fun GIVEN_discover_services_without_error_WHEN_services_exist_THEN_discover_characteristics_per_service() =
        runTest {
            val sut = createSut()
            val service1 = newService(META_SERVICE_SHORT_UUID, emptyList())
            val service2 = newService(SERIAL_SERVICE_SHORT_UUID, emptyList())
            sut.peripheral.setServicesRaw(listOf(service1, service2))

            sut.sut.handleDidDiscoverServices(sut.peripheral, didDiscoverServices = null)

            assertEquals(2, sut.peripheral.discoverCharacteristicsRequests.size)
        }

    @Test
    fun GIVEN_discover_services_error_WHEN_callback_received_THEN_no_characteristics_discovery_requested() = runTest {
        val sut = createSut()

        sut.sut.handleDidDiscoverServices(
            peripheral = sut.peripheral,
            didDiscoverServices = error(domain = "CBErrorDomain", code = 1L),
        )

        assertTrue(sut.peripheral.discoverCharacteristicsRequests.isEmpty())
    }

    @Test
    fun GIVEN_cb_errors_WHEN_onError_invoked_THEN_state_is_mapped_for_pairing_and_disconnect_errors() = runTest {
        val sut = createSut().sut

        sut.onError(error(domain = "CBATTErrorDomain", code = 15L))
        assertEquals(FPeripheralState.PAIRING_FAILED, sut.stateStream.value)

        sut.onError(error(domain = "CBErrorDomain", code = 7L))
        assertEquals(FPeripheralState.INVALID_PAIRING, sut.stateStream.value)

        sut.onError(error(domain = "CBErrorDomain", code = 17L))
        assertEquals(FPeripheralState.DISCONNECTED, sut.stateStream.value)
    }

    @Test
    fun GIVEN_serial_characteristics_and_meta_value_WHEN_updated_THEN_write_and_connected_state_work() =
        runTest {
            val sut = createSut()
            val rx = newCharacteristic(SERIAL_RX_SHORT_UUID)
            val tx = newCharacteristic(SERIAL_TX_SHORT_UUID)
            val serialService = newService(SERIAL_SERVICE_SHORT_UUID, listOf(rx, tx))

            sut.sut.didDiscoverCharacteristics(serialService, error = null)
            assertTrue(
                sut.peripheral.notifyRequests.any { (enabled, uuid) ->
                    enabled && uuid.equals(SERIAL_RX_SHORT_UUID, ignoreCase = true)
                }
            )

            val metaPayload = "connected".encodeToByteArray()
            val metaChar = newCharacteristic(DEVICE_NAME_SHORT_UUID, payload = metaPayload)
            sut.sut.didUpdateValue(metaChar, error = null)
            assertEquals(FPeripheralState.CONNECTED, sut.sut.stateStream.value)

            val writePayload = byteArrayOf(0x1, 0x2, 0x3)
            val writeJob = async { sut.sut.writeValue(writePayload) }
            runCurrent()
            assertEquals(1, sut.peripheral.writeRequests.size)

            sut.sut.handleDidWriteValue(tx, error = null)
            writeJob.await()
            assertContentEquals(writePayload, sut.peripheral.writeRequests.single().value)
        }

    @Test
    fun GIVEN_not_connected_state_WHEN_writeValue_called_THEN_write_is_ignored() = runTest {
        val sut = createSut()
        val rx = newCharacteristic(SERIAL_RX_SHORT_UUID)
        val tx = newCharacteristic(SERIAL_TX_SHORT_UUID)
        val serialService = newService(SERIAL_SERVICE_SHORT_UUID, listOf(rx, tx))
        sut.sut.didDiscoverCharacteristics(serialService, error = null)

        sut.sut.writeValue(byteArrayOf(1, 2, 3))

        assertTrue(sut.peripheral.writeRequests.isEmpty())
    }

    @Test
    fun GIVEN_connected_state_without_ack_WHEN_writeValue_called_THEN_timeout_is_propagated() = runTest {
        val sut = createConnectedSut()

        val writeJob = async { sut.sut.writeValue(byteArrayOf(5, 4, 3, 2, 1)) }
        runCurrent()
        assertEquals(1, sut.peripheral.writeRequests.size)

        advanceTimeBy(WRITE_ACK_TIMEOUT_MS + 1)
        assertFailsWith<TimeoutCancellationException> {
            writeJob.await()
        }
    }

    @Test
    fun GIVEN_non_matching_ack_WHEN_waiting_for_write_response_THEN_ignore_until_matching_characteristic() =
        runTest {
            val sut = createConnectedSut()
            val writeJob = async { sut.sut.writeValue(byteArrayOf(5, 4, 3, 2, 1)) }
            runCurrent()
            assertEquals(1, sut.peripheral.writeRequests.size)

            sut.sut.handleDidWriteValue(newCharacteristic(DEVICE_NAME_SHORT_UUID), error = null)
            runCurrent()
            assertFalse(writeJob.isCompleted)

            sut.sut.handleDidWriteValue(sut.tx, error = null)
            writeJob.await()
        }

    @Test
    fun GIVEN_large_payload_WHEN_writeValue_called_THEN_data_is_chunked_and_each_chunk_waits_for_ack() = runTest {
        val sut = createConnectedSut()
        val payload = ByteArray(MAX_ATTRIBUTE_SIZE * 2 + 10) { it.toByte() }

        val writeJob = async { sut.sut.writeValue(payload) }

        runCurrent()
        assertEquals(listOf(MAX_ATTRIBUTE_SIZE), sut.peripheral.writeRequests.map { it.value.size })

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        runCurrent()
        assertEquals(
            listOf(MAX_ATTRIBUTE_SIZE, MAX_ATTRIBUTE_SIZE),
            sut.peripheral.writeRequests.map { it.value.size }
        )

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        runCurrent()
        assertEquals(
            listOf(MAX_ATTRIBUTE_SIZE, MAX_ATTRIBUTE_SIZE, 10),
            sut.peripheral.writeRequests.map { it.value.size }
        )

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        writeJob.await()
    }

    @Test
    fun GIVEN_two_large_write_calls_WHEN_started_together_THEN_second_waits_until_first_fully_acked() = runTest {
        val sut = createConnectedSut()
        val firstPayload = ByteArray(MAX_ATTRIBUTE_SIZE * 2 + 1) { it.toByte() }
        val secondPayload = ByteArray(MAX_ATTRIBUTE_SIZE + 2) { (it + 1).toByte() }

        val firstWriteJob = async { sut.sut.writeValue(firstPayload) }
        val secondWriteJob = async { sut.sut.writeValue(secondPayload) }

        runCurrent()
        assertEquals(listOf(MAX_ATTRIBUTE_SIZE), sut.peripheral.writeRequests.map { it.value.size })

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        runCurrent()
        assertEquals(
            listOf(MAX_ATTRIBUTE_SIZE, MAX_ATTRIBUTE_SIZE),
            sut.peripheral.writeRequests.map { it.value.size }
        )
        assertFalse(secondWriteJob.isCompleted)

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        runCurrent()
        assertEquals(
            listOf(MAX_ATTRIBUTE_SIZE, MAX_ATTRIBUTE_SIZE, 1),
            sut.peripheral.writeRequests.map { it.value.size }
        )
        assertFalse(secondWriteJob.isCompleted)

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        runCurrent()
        assertEquals(
            listOf(MAX_ATTRIBUTE_SIZE, MAX_ATTRIBUTE_SIZE, 1, MAX_ATTRIBUTE_SIZE),
            sut.peripheral.writeRequests.map { it.value.size }
        )

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        runCurrent()
        assertEquals(
            listOf(MAX_ATTRIBUTE_SIZE, MAX_ATTRIBUTE_SIZE, 1, MAX_ATTRIBUTE_SIZE, 2),
            sut.peripheral.writeRequests.map { it.value.size }
        )

        sut.sut.handleDidWriteValue(sut.tx, error = null)
        firstWriteJob.await()
        secondWriteJob.await()
    }

    @Test
    fun GIVEN_pending_write_WHEN_disconnect_happens_THEN_writer_gets_cancellation_and_cleanup_is_applied() = runTest {
        val sut = createConnectedSut()

        val writeJob = async { sut.sut.writeValue(byteArrayOf(1, 2, 3)) }
        runCurrent()
        assertEquals(1, sut.peripheral.writeRequests.size)

        sut.sut.onDisconnect()

        assertFailsWith<CancellationException> {
            writeJob.await()
        }
        assertEquals(FPeripheralState.DISCONNECTED, sut.sut.stateStream.value)
        assertTrue(sut.sut.metaInfoKeysStream.value.isEmpty())
    }

    @Test
    fun GIVEN_pairing_failed_state_WHEN_onDisconnect_called_THEN_state_and_meta_are_kept() = runTest {
        val sut = createConnectedSut()
        assertTrue(sut.sut.metaInfoKeysStream.value.isNotEmpty())

        sut.sut.onError(error(domain = "CBATTErrorDomain", code = 15L))
        assertEquals(FPeripheralState.PAIRING_FAILED, sut.sut.stateStream.value)

        sut.sut.onDisconnect()

        assertEquals(FPeripheralState.PAIRING_FAILED, sut.sut.stateStream.value)
        assertTrue(sut.sut.metaInfoKeysStream.value.isNotEmpty())
    }

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
    fun GIVEN_meta_characteristic_update_WHEN_didUpdateValue_called_THEN_meta_updates_and_state_connected() = runTest {
        val sut = createSut().sut
        val payload = "BusyBar".encodeToByteArray()
        val metaCharacteristic = newCharacteristic(DEVICE_NAME_SHORT_UUID, payload = payload)

        sut.didUpdateValue(metaCharacteristic, error = null)

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

        assertEquals(FPeripheralState.PAIRING_FAILED, sut.stateStream.value)
    }

    @Test
    fun GIVEN_meta_service_known_and_unknown_chars_WHEN_discovered_THEN_known_reads_and_battery_subscribed() =
        runTest {
            val config = createConfig(
                macAddress = RecordingPeripheral().identifier.UUIDString,
                metaInfoGattMap = batteryAndManufacturerMetaMap(),
            )
            val peripheral = RecordingPeripheral()
            val sut = FPeripheral(peripheral, config, this)

            val battery = newCharacteristic(BATTERY_LEVEL_SHORT_UUID)
            val manufacturer = newCharacteristic(MANUFACTURER_SHORT_UUID)
            val unknown = newCharacteristic("2A99")
            val metaService = newService(META_SERVICE_SHORT_UUID, listOf(battery, manufacturer, unknown))

            sut.didDiscoverCharacteristics(metaService, error = null)

            assertTrue(peripheral.readRequests.any { it.equals(BATTERY_LEVEL_SHORT_UUID, ignoreCase = true) })
            assertTrue(peripheral.readRequests.any { it.equals(MANUFACTURER_SHORT_UUID, ignoreCase = true) })
            assertFalse(peripheral.readRequests.any { it.equals("2A99", ignoreCase = true) })

            assertTrue(
                peripheral.notifyRequests.any { (enabled, uuid) ->
                    enabled && uuid.equals(BATTERY_LEVEL_SHORT_UUID, ignoreCase = true)
                }
            )
            assertFalse(
                peripheral.notifyRequests.any { (_, uuid) ->
                    uuid.equals(MANUFACTURER_SHORT_UUID, ignoreCase = true)
                }
            )
        }

    private data class Sut(
        val peripheral: RecordingPeripheral,
        val sut: FPeripheral,
    )

    private data class ConnectedSut(
        val peripheral: RecordingPeripheral,
        val sut: FPeripheral,
        val tx: platform.CoreBluetooth.CBCharacteristic,
    )

    private fun TestScope.createSut(): Sut {
        val peripheral = RecordingPeripheral().apply { setStateRaw(0L) }
        val sut = FPeripheral(
            peripheral = peripheral,
            config = createConfig(macAddress = peripheral.identifier.UUIDString),
            scope = this,
        )
        return Sut(peripheral = peripheral, sut = sut)
    }

    private suspend fun TestScope.createConnectedSut(): ConnectedSut {
        val base = createSut()

        val rx = newCharacteristic(SERIAL_RX_SHORT_UUID)
        val tx = newCharacteristic(SERIAL_TX_SHORT_UUID)
        val serialService = newService(SERIAL_SERVICE_SHORT_UUID, listOf(rx, tx))
        base.sut.didDiscoverCharacteristics(serialService, error = null)

        val metaPayload = "ready".encodeToByteArray()
        val metaChar = newCharacteristic(DEVICE_NAME_SHORT_UUID, payload = metaPayload)
        base.sut.didUpdateValue(metaChar, error = null)

        return ConnectedSut(
            peripheral = base.peripheral,
            sut = base.sut,
            tx = tx,
        )
    }
}
