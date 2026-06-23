package net.flipper.bridge.connection.transport.ble.impl.ios.central

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralState
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.RecordingCentralManager
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.RecordingPeripheral
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_SERVICE_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.createConfig
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.error
import platform.CoreBluetooth.CBErrorPeerRemovedPairingInformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FCentralManagerTest {

    @Test
    fun GIVEN_unknown_peripheral_WHEN_connect_called_THEN_request_is_ignored() = runTest {
        val sut = createSut()
        val config = createConfig(macAddress = "00000000-0000-0000-0000-000000000001")

        sut.sut.connect(backgroundScope, config)

        assertTrue(sut.manager.connectRequests.isEmpty())
        assertTrue(sut.sut.connectedStream.value.isEmpty())
    }

    @Test
    fun GIVEN_known_peripheral_WHEN_connect_called_THEN_connecting_device_added_and_connect_requested() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)

        sut.sut.connect(backgroundScope, createConfig(macAddress = peripheral.identifier.UUIDString))

        assertEquals(1, sut.manager.connectRequests.size)
        assertSame(sut.manager.connectRequests.first(), peripheral)
        val connected = sut.sut.connectedStream.value[peripheral.identifier]
        assertNotNull(connected)
        assertEquals(FPeripheralState.CONNECTING, connected.stateStream.value)
    }

    @Test
    fun GIVEN_connected_entry_WHEN_didConnect_callback_arrives_THEN_fperipheral_onConnect_is_invoked() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(backgroundScope, createConfig(macAddress = peripheral.identifier.UUIDString))

        sut.manager.emitDidConnect(peripheral)
        advanceUntilIdle()

        assertEquals(1, peripheral.discoverServicesCalls)
    }

    @Test
    fun GIVEN_connected_entry_WHEN_disconnect_callback_arrives_THEN_entry_is_removed() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(backgroundScope, createConfig(macAddress = peripheral.identifier.UUIDString))
        val device = checkNotNull(sut.sut.connectedStream.value[peripheral.identifier])

        val disconnectJob = async { sut.sut.disconnect(peripheral.identifier) }
        runCurrent()

        assertEquals(1, sut.manager.cancelRequests.size)
        assertTrue(sut.manager.cancelRequests.first() === peripheral)
        assertEquals(FPeripheralState.DISCONNECTING, device.stateStream.value)

        sut.manager.emitDidDisconnect(peripheral, error = null)
        advanceUntilIdle()
        disconnectJob.await()

        assertFalse(sut.sut.connectedStream.value.containsKey(peripheral.identifier))
        assertEquals(FPeripheralState.DISCONNECTED, device.stateStream.value)
    }

    @Test
    fun GIVEN_connecting_peripheral_WHEN_connection_scope_cancelled_THEN_cb_connection_is_torn_down() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)

        // The connection scope is shared down into FPeripheral and its BLEEventQueue consumer.
        val connectionJob = Job()
        val connectionScope = CoroutineScope(coroutineContext + connectionJob)
        sut.sut.connect(connectionScope, createConfig(macAddress = peripheral.identifier.UUIDString))
        val device = checkNotNull(sut.sut.connectedStream.value[peripheral.identifier])
        sut.manager.emitDidConnect(peripheral)
        advanceUntilIdle()
        // The proper disconnect() is never called for an in-progress connection: it lives in
        // FIOSBleApiImpl, created only after CONNECTED. Without the scope-completion hook the
        // CBPeripheral would stay connected and keep streaming into a queue whose consumer died
        // with the scope, eventually overflowing and crashing the app.
        assertTrue(sut.manager.cancelRequests.isEmpty(), "No teardown should happen before cancellation")

        // Fast disconnect: the shared connection scope is cancelled while still connecting.
        connectionJob.cancel()
        advanceUntilIdle()

        assertTrue(
            sut.manager.cancelRequests.any { it === peripheral },
            "Cancelling the connection scope mid-connect must request CB disconnect"
        )

        // CoreBluetooth delivers didDisconnect in response to cancelPeripheralConnection,
        // which fully tears the peripheral down.
        sut.manager.emitDidDisconnect(peripheral, error = null)
        advanceUntilIdle()

        assertFalse(sut.sut.connectedStream.value.containsKey(peripheral.identifier))
        assertEquals(FPeripheralState.DISCONNECTED, device.stateStream.value)
    }

    @Test
    fun GIVEN_pairing_error_on_connect_WHEN_callback_arrives_THEN_entry_removed_and_state_updated() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(backgroundScope, createConfig(macAddress = peripheral.identifier.UUIDString))
        val device = checkNotNull(sut.sut.connectedStream.value[peripheral.identifier])

        sut.manager.emitDidFailToConnect(
            peripheral,
            error = error(domain = "CBErrorDomain", code = CBErrorPeerRemovedPairingInformation)
        )
        // onError schedules state via scope.launch on backgroundScope — wait for the transition.
        device.stateStream.first { it == FPeripheralState.DEVICE_FORGOT_PAIRING }
        sut.sut.connectedStream.first { !it.containsKey(peripheral.identifier) }

        assertEquals(FPeripheralState.DEVICE_FORGOT_PAIRING, device.stateStream.value)
        assertFalse(sut.sut.connectedStream.value.containsKey(peripheral.identifier))
    }

    @Test
    fun GIVEN_failed_connect_without_error_WHEN_callback_arrives_THEN_entry_is_still_cleaned_up() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(backgroundScope, createConfig(macAddress = peripheral.identifier.UUIDString))

        sut.manager.emitDidFailToConnect(peripheral, error = null)
        advanceUntilIdle()

        assertFalse(
            sut.sut.connectedStream.value.containsKey(peripheral.identifier),
            "A failed connection without NSError should still be removed from connectedStream"
        )
    }

    @Test
    fun GIVEN_ble_not_powered_on_WHEN_startScan_called_THEN_scan_is_not_started() = runTest {
        val sut = createSut()
        sut.manager.setStateRaw(4L)

        sut.sut.startScan()

        assertTrue(sut.manager.scanRequests.isEmpty())
    }

    @Test
    fun GIVEN_manager_powered_on_WHEN_first_sut_call_THEN_ble_status_stream_is_synchronized() =
        runTest {
            val manager = RecordingCentralManager().apply {
                setStateRaw(5L)
            }

            val childJob = Job()
            val childScope = CoroutineScope(coroutineContext + childJob)
            backgroundScope.coroutineContext[Job]!!.invokeOnCompletion { childJob.cancel() }
            val sut = FCentralManager(
                scope = childScope,
                centralManagerProvider = { delegate -> manager.also { it.delegate = delegate } }
            )

            manager.emitStateUpdate()
            advanceUntilIdle()

            assertEquals(FBLEStatus.POWERED_ON, sut.bleStatusStream.value)
        }

    @Test
    fun GIVEN_ble_powered_on_WHEN_startScan_called_THEN_scan_is_started_for_expected_service() = runTest {
        val sut = createSut()
        sut.manager.setStateRaw(5L)
        sut.manager.emitStateUpdate()
        advanceUntilIdle()

        sut.sut.startScan()

        assertEquals(1, sut.manager.scanRequests.size)
        val requestedServices = sut.manager.scanRequests.single()
        assertTrue(
            requestedServices.any {
                (it as platform.CoreBluetooth.CBUUID).UUIDString.equals(SERIAL_SERVICE_SHORT_UUID, ignoreCase = true)
            }
        )
    }

    @Test
    fun GIVEN_scanning_with_devices_WHEN_stopScan_called_THEN_scanning_stops_and_discovered_clears() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()

        sut.manager.setStateRaw(5L)
        sut.manager.emitStateUpdate()
        advanceUntilIdle()
        sut.sut.startScan()
        sut.manager.emitDidDiscover(peripheral)
        advanceUntilIdle()
        assertEquals(1, sut.sut.discoveredStream.value.size)

        sut.sut.stopScan()

        assertEquals(1, sut.manager.stopScanCalls)
        assertTrue(sut.sut.discoveredStream.value.isEmpty())
    }

    @Test
    fun GIVEN_same_device_discovered_multiple_times_WHEN_callbacks_arrive_THEN_discovered_set_stays_unique() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()

        sut.manager.emitDidDiscover(peripheral)
        sut.manager.emitDidDiscover(peripheral)
        advanceUntilIdle()

        assertEquals(1, sut.sut.discoveredStream.value.size)
    }

    @Test
    fun GIVEN_connected_and_discovered_entries_WHEN_ble_powers_off_THEN_everything_disconnects_and_clears() =
        runTest {
            val sut = createSut()
            val peripheral = RecordingPeripheral()
            sut.manager.registerPeripheral(peripheral)
            sut.sut.connect(backgroundScope, createConfig(macAddress = peripheral.identifier.UUIDString))
            val device = checkNotNull(sut.sut.connectedStream.value[peripheral.identifier])

            sut.manager.emitDidDiscover(peripheral)
            advanceUntilIdle()
            assertFalse(sut.sut.discoveredStream.value.isEmpty())

            sut.manager.setStateRaw(4L)
            sut.manager.emitStateUpdate()
            advanceUntilIdle()

            assertEquals(FBLEStatus.POWERED_OFF, sut.sut.bleStatusStream.value)
            assertTrue(sut.sut.connectedStream.value.isEmpty())
            assertTrue(sut.sut.discoveredStream.value.isEmpty())
            assertEquals(FPeripheralState.DISCONNECTED, device.stateStream.value)
        }

    private data class Sut(
        val manager: RecordingCentralManager,
        val sut: FCentralManager,
    )

    private suspend fun TestScope.createSut(): Sut {
        val manager = RecordingCentralManager()
        val childJob = Job()
        val childScope = CoroutineScope(coroutineContext + childJob)
        backgroundScope.coroutineContext[Job]!!.invokeOnCompletion { childJob.cancel() }
        val sut = FCentralManager(
            scope = childScope,
            centralManagerProvider = { delegate -> manager.also { it.delegate = delegate } }
        )
        return Sut(manager = manager, sut = sut)
    }
}
