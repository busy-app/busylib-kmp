package net.flipper.transport.ble.impl.cb

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FCentralManagerTest {

    @Test
    fun GIVEN_unknown_peripheral_WHEN_connect_called_THEN_request_is_ignored() = runTest {
        val sut = createSut()
        val config = createConfig(macAddress = "00000000-0000-0000-0000-000000000001")

        sut.sut.connect(config)

        assertTrue(sut.manager.connectRequests.isEmpty())
        assertTrue(sut.sut.connectedStream.value.isEmpty())
    }

    @Test
    fun GIVEN_known_peripheral_WHEN_connect_called_THEN_connecting_device_added_and_connect_requested() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)

        sut.sut.connect(createConfig(macAddress = peripheral.identifier.UUIDString))

        assertEquals(1, sut.manager.connectRequests.size)
        assertTrue(sut.manager.connectRequests.first() === peripheral)
        val connected = sut.sut.connectedStream.value[peripheral.identifier]
        assertNotNull(connected)
        assertEquals(FPeripheralState.CONNECTING, connected.stateStream.value)
    }

    @Test
    fun GIVEN_connected_entry_WHEN_didConnect_callback_arrives_THEN_fperipheral_onConnect_is_invoked() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(createConfig(macAddress = peripheral.identifier.UUIDString))

        sut.manager.emitDidConnect(peripheral)
        advanceUntilIdle()

        assertEquals(1, peripheral.discoverServicesCalls)
    }

    @Test
    fun GIVEN_connected_entry_WHEN_disconnect_callback_arrives_THEN_entry_is_removed() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(createConfig(macAddress = peripheral.identifier.UUIDString))
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
    fun GIVEN_pairing_error_on_connect_WHEN_callback_arrives_THEN_entry_removed_and_state_updated() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(createConfig(macAddress = peripheral.identifier.UUIDString))
        val device = checkNotNull(sut.sut.connectedStream.value[peripheral.identifier])

        sut.manager.emitDidFailToConnect(peripheral, error = error(domain = "CBErrorDomain", code = 7L))
        advanceUntilIdle()

        assertEquals(FPeripheralState.INVALID_PAIRING, device.stateStream.value)
        assertFalse(sut.sut.connectedStream.value.containsKey(peripheral.identifier))
    }

    @Test
    fun GIVEN_failed_connect_without_error_WHEN_callback_arrives_THEN_entry_is_still_cleaned_up() = runTest {
        val sut = createSut()
        val peripheral = RecordingPeripheral()
        sut.manager.registerPeripheral(peripheral)
        sut.sut.connect(createConfig(macAddress = peripheral.identifier.UUIDString))

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
    fun GIVEN_ble_powered_on_WHEN_startScan_called_THEN_scan_is_started_for_expected_service() = runTest {
        val sut = createSut()
        sut.manager.setStateRaw(5L)

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
            sut.sut.connect(createConfig(macAddress = peripheral.identifier.UUIDString))
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

    private fun TestScope.createSut(): Sut {
        val manager = RecordingCentralManager()
        val sut = FCentralManager(
            scope = this,
            manager = manager,
        )
        return Sut(manager = manager, sut = sut)
    }
}
