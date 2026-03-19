package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.api.MAX_ATTRIBUTE_SIZE
import net.flipper.bridge.connection.transport.ble.api.WRITE_ACK_TIMEOUT_MS
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.DEVICE_NAME_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_RESET_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_TX_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.error
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.newCharacteristic
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FPeripheralGattIOTest {

    @Test
    fun GIVEN_not_connected_state_WHEN_writeValue_called_THEN_write_is_ignored() = runTest {
        val sut = createSut()

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
    fun GIVEN_non_matching_ack_WHEN_waiting_for_serial_write_response_THEN_ignore_until_matching_characteristic() =
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
    fun GIVEN_reset_characteristic_discovered_WHEN_service_processed_THEN_generic_read_write_uses_it() =
        runTest {
            val sut = createConnectedSut()
            val resetUuid = sut.config.serialConfig.resetCharUuid
            val payload = byteArrayOf(0x3, 0x2, 0x1, 0x0)

            val readJob = async { sut.sut.readValue(resetUuid) }
            runCurrent()
            assertTrue(
                sut.peripheral.readRequests.any { uuid ->
                    uuid.equals(SERIAL_RESET_UUID, ignoreCase = true)
                }
            )

            sut.sut.didUpdateValue(newCharacteristic(SERIAL_RESET_UUID, payload = payload), error = null)
            assertContentEquals(payload, readJob.await())

            val writePayload = byteArrayOf(0, 0, 0, 0)
            val writeJob = async { sut.sut.writeValue(resetUuid, writePayload) }
            runCurrent()
            assertTrue(
                sut.peripheral.writeRequests.any { request ->
                    request.characteristicUuid.equals(SERIAL_RESET_UUID, ignoreCase = true)
                }
            )

            sut.sut.handleDidWriteValue(newCharacteristic(SERIAL_RESET_UUID), error = null)
            writeJob.await()
        }

    @Test
    fun GIVEN_readValue_called_for_reset_WHEN_didUpdateValue_arrives_THEN_waiter_receives_payload() = runTest {
        val sut = createConnectedSut()
        val resetUuid = sut.config.serialConfig.resetCharUuid
        val payload = byteArrayOf(0x1, 0x0, 0x0, 0x0)

        val readJob = async { sut.sut.readValue(resetUuid) }
        runCurrent()
        sut.sut.didUpdateValue(newCharacteristic(SERIAL_RESET_UUID, payload = payload), error = null)

        assertContentEquals(payload, readJob.await())
    }

    @Test
    fun GIVEN_readValue_called_for_reset_without_update_WHEN_timeout_passes_THEN_timeout_is_propagated() = runTest {
        val sut = createConnectedSut()
        val resetUuid = sut.config.serialConfig.resetCharUuid

        val readJob = async { sut.sut.readValue(resetUuid) }
        runCurrent()

        advanceTimeBy(WRITE_ACK_TIMEOUT_MS + 1)
        assertFailsWith<TimeoutCancellationException> {
            readJob.await()
        }
    }

    @Test
    fun GIVEN_writeValue_called_for_reset_WHEN_matching_write_ack_arrives_THEN_call_completes() = runTest {
        val sut = createConnectedSut()
        val resetUuid = sut.config.serialConfig.resetCharUuid
        val payload = byteArrayOf(0, 0, 0, 0)

        val writeJob = async { sut.sut.writeValue(resetUuid, payload) }
        runCurrent()

        assertTrue(
            sut.peripheral.writeRequests.any { request ->
                request.characteristicUuid.equals(SERIAL_RESET_UUID, ignoreCase = true)
            }
        )

        sut.sut.handleDidWriteValue(newCharacteristic(SERIAL_RESET_UUID), error = null)
        writeJob.await()
    }

    @Test
    fun GIVEN_matching_write_ack_with_error_WHEN_waiting_for_write_response_THEN_call_fails() = runTest {
        val sut = createConnectedSut()
        val resetUuid = sut.config.serialConfig.resetCharUuid
        val writeFailure = CompletableDeferred<Throwable?>()

        val writeJob = backgroundScope.launch {
            try {
                sut.sut.writeValue(resetUuid, byteArrayOf(0, 0, 0, 0))
                writeFailure.complete(null)
            } catch (throwable: Throwable) {
                writeFailure.complete(throwable)
            }
        }
        runCurrent()

        sut.sut.handleDidWriteValue(
            didWriteValueForCharacteristic = newCharacteristic(SERIAL_RESET_UUID),
            error = error(domain = "CBErrorDomain", code = 15L),
        )
        runCurrent()

        val throwable = writeFailure.await()
        assertIs<Exception>(throwable)
        writeJob.cancel()
    }

    @Test
    fun GIVEN_pending_generic_read_or_write_WHEN_disconnect_happens_THEN_waiters_fail_and_cleanup_happens() =
        runTest {
            val sut = createConnectedSut()
            val resetUuid = sut.config.serialConfig.resetCharUuid

            val readJob = async { sut.sut.readValue(resetUuid) }
            val writeJob = async { sut.sut.writeValue(resetUuid, byteArrayOf(0, 0, 0, 0)) }
            runCurrent()

            sut.sut.onDisconnect()

            assertFailsWith<CancellationException> { readJob.await() }
            assertFailsWith<CancellationException> { writeJob.await() }

            assertFailsWith<IllegalStateException> {
                sut.sut.readValue(resetUuid)
            }
            assertFailsWith<IllegalStateException> {
                sut.sut.writeValue(resetUuid, byteArrayOf(0, 0, 0, 0))
            }
        }

    @Test
    fun GIVEN_non_matching_ack_WHEN_waiting_for_reset_write_THEN_ignore_until_matching_uuid() = runTest {
        val sut = createConnectedSut()
        val resetUuid = sut.config.serialConfig.resetCharUuid
        val writeJob = async { sut.sut.writeValue(resetUuid, byteArrayOf(0, 0, 0, 0)) }
        runCurrent()
        assertEquals(1, sut.peripheral.writeRequests.size)

        sut.sut.handleDidWriteValue(newCharacteristic(SERIAL_TX_SHORT_UUID), error = null)
        runCurrent()
        assertFalse(writeJob.isCompleted)

        sut.sut.handleDidWriteValue(newCharacteristic(SERIAL_RESET_UUID), error = null)
        writeJob.await()
    }
}
