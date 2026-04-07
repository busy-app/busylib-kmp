package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.RecordingPeripheral
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_RX_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.createConfig
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.error
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.newCharacteristic
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import platform.CoreBluetooth.CBATTErrorInsufficientEncryption
import platform.CoreBluetooth.CBErrorEncryptionTimedOut
import platform.CoreBluetooth.CBErrorPeerRemovedPairingInformation
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FPeripheralLifecycleTest {

    @Test
    fun GIVEN_initial_peripheral_state_WHEN_creating_fperipheral_THEN_stateStream_is_initialized_from_cb_state() =
        runTest {
            val recordingPeripheral = RecordingPeripheral().apply { setStateRaw(2L) }
            val sut = FPeripheral(
                recordingPeripheral,
                createConfig(recordingPeripheral.identifier.UUIDString),
                this,
            )

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
    fun GIVEN_cb_errors_WHEN_onError_invoked_THEN_state_is_mapped_for_pairing_and_disconnect_errors() = runTest {
        val sut = createSut().sut

        // onError dispatches state updates on Dispatchers.Default, so await the state transition
        sut.onError(error(domain = "CBATTErrorDomain", code = CBATTErrorInsufficientEncryption))
        sut.stateStream.first { it == FPeripheralState.PAIRING_FAILED }

        sut.onError(error(domain = "CBErrorDomain", code = CBErrorPeerRemovedPairingInformation))
        sut.stateStream.first { it == FPeripheralState.INVALID_PAIRING }

        sut.onError(error(domain = "CBErrorDomain", code = CBErrorEncryptionTimedOut))
        sut.stateStream.first { it == FPeripheralState.DISCONNECTED }
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

        sut.sut.onError(error(domain = "CBATTErrorDomain", code = CBATTErrorInsufficientEncryption))
        sut.sut.stateStream.first { it == FPeripheralState.PAIRING_FAILED }

        sut.sut.onDisconnect()

        assertEquals(FPeripheralState.PAIRING_FAILED, sut.sut.stateStream.value)
        assertTrue(sut.sut.metaInfoKeysStream.value.isNotEmpty())
    }

    @Test
    fun GIVEN_connected_peripheral_WHEN_disconnect_called_THEN_cleanup_is_applied_and_new_ops_suspend() =
        runTest {
            val connected = createConnectedSut()
            val resetUuid = connected.config.serialConfig.resetCharUuid

            connected.sut.onDisconnect()
            runCurrent()

            assertEquals(FPeripheralState.DISCONNECTED, connected.sut.stateStream.value)
            assertEquals(emptyMap<TransportMetaInfoKey, ByteArray?>(), connected.sut.metaInfoKeysStream.value)

            // After disconnect, new operations suspend at waitConnected()
            val readJob = async { connected.sut.readValue(resetUuid) }
            val writeJob = async { connected.sut.writeValue(resetUuid, byteArrayOf(0, 0, 0, 0)) }
            runCurrent()
            assertFalse(readJob.isCompleted)
            assertFalse(writeJob.isCompleted)
            readJob.cancel()
            writeJob.cancel()
        }

    @Test
    fun GIVEN_active_rx_collector_WHEN_disconnect_called_THEN_rx_flow_completes_with_buffered_data() = runTest {
        val connected = createConnectedSut()
        val payload = byteArrayOf(1, 2, 3)

        val collectJob = async {
            connected.sut.rxDataStream.toList()
        }
        runCurrent()

        connected.sut.didUpdateValue(
            newCharacteristic(SERIAL_RX_SHORT_UUID, payload = payload),
            error = null,
        )
        connected.sut.onDisconnect()
        runCurrent()

        val received = collectJob.await()

        assertEquals(1, received.size)
        assertContentEquals(payload, received[0])
    }

    @Test
    fun GIVEN_no_rx_data_sent_WHEN_disconnect_called_THEN_rx_flow_completes_empty() = runTest {
        val connected = createConnectedSut()

        val collectJob = async {
            connected.sut.rxDataStream.toList()
        }
        runCurrent()

        connected.sut.onDisconnect()
        runCurrent()

        val received = collectJob.await()

        assertTrue(received.isEmpty())
    }
}
