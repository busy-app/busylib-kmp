package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.BATTERY_LEVEL_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.DEVICE_NAME_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.MANUFACTURER_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.META_SERVICE_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.RecordingPeripheral
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_RX_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_SERVICE_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_TX_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.batteryAndManufacturerMetaMap
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.createConfig
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.error
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.newCharacteristic
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.newService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FPeripheralDiscoveryTest {

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
    fun GIVEN_serial_service_WHEN_discovered_THEN_rx_notify_is_enabled_and_tx_becomes_write_target() = runTest {
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

        sut.sut.didUpdateValue(
            newCharacteristic(DEVICE_NAME_SHORT_UUID, payload = "connected".encodeToByteArray()),
            error = null,
        )
        val writeJob = async { sut.sut.writeValue(byteArrayOf(1, 2, 3)) }
        runCurrent()
        assertTrue(sut.peripheral.writeRequests.isNotEmpty())
        sut.sut.handleDidWriteValue(tx, error = null)
        writeJob.await()
    }

    @Test
    fun GIVEN_meta_chars_discovered_WHEN_processing_THEN_only_known_are_read_and_battery_subscribed() =
        runTest {
            val config = createConfig(
                macAddress = RecordingPeripheral().identifier.UUIDString,
                metaInfoGattMap = batteryAndManufacturerMetaMap(),
            )
            val sut = createSut(config = config)

            val battery = newCharacteristic(BATTERY_LEVEL_SHORT_UUID)
            val manufacturer = newCharacteristic(MANUFACTURER_SHORT_UUID)
            val unknown = newCharacteristic("2A99")
            val metaService = newService(META_SERVICE_SHORT_UUID, listOf(battery, manufacturer, unknown))

            sut.sut.didDiscoverCharacteristics(metaService, error = null)

            assertTrue(sut.peripheral.readRequests.any { it.equals(BATTERY_LEVEL_SHORT_UUID, ignoreCase = true) })
            assertTrue(sut.peripheral.readRequests.any { it.equals(MANUFACTURER_SHORT_UUID, ignoreCase = true) })
            assertFalse(sut.peripheral.readRequests.any { it.equals("2A99", ignoreCase = true) })

            assertTrue(
                sut.peripheral.notifyRequests.any { (enabled, uuid) ->
                    enabled && uuid.equals(BATTERY_LEVEL_SHORT_UUID, ignoreCase = true)
                }
            )
            assertFalse(
                sut.peripheral.notifyRequests.any { (_, uuid) ->
                    uuid.equals(MANUFACTURER_SHORT_UUID, ignoreCase = true)
                }
            )
        }

    @Test
    fun GIVEN_characteristics_discover_error_WHEN_callback_received_THEN_no_reads_or_subscriptions_are_registered() =
        runTest {
            val sut = createSut()

            val service = newService(
                SERIAL_SERVICE_SHORT_UUID,
                listOf(newCharacteristic(SERIAL_RX_SHORT_UUID), newCharacteristic(SERIAL_TX_SHORT_UUID)),
            )

            sut.sut.didDiscoverCharacteristics(
                service = service,
                error = error(domain = "CBErrorDomain", code = 8L),
            )

            assertTrue(sut.peripheral.readRequests.isEmpty())
            assertTrue(sut.peripheral.notifyRequests.isEmpty())

            sut.sut.didUpdateValue(
                newCharacteristic(DEVICE_NAME_SHORT_UUID, payload = "connected".encodeToByteArray()),
                error = null,
            )
            sut.sut.writeValue(byteArrayOf(9, 9, 9))
            assertTrue(sut.peripheral.writeRequests.isEmpty())
        }
}
