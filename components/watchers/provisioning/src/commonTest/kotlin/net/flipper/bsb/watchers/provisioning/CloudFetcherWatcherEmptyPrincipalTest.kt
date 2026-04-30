package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.watchers.provisioning.fakes.busyBar
import net.flipper.bsb.watchers.provisioning.fakes.createCloudFetcherSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class CloudFetcherWatcherEmptyPrincipalTest {

    @Test
    fun GIVEN_empty_principal_and_no_cloud_devices_THEN_no_storage_mutation() = runTest {
        val bleDevice = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(bleDevice),
            isNetworkAvailable = true,
            principal = BUSYLibUserPrincipal.Empty
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(bleDevice), setup.storage.devices.value)
    }

    @Test
    fun GIVEN_empty_principal_with_cloud_only_device_THEN_device_removed() = runTest {
        val cloudId = Uuid.random()
        val cloudOnly = busyBar(
            id = "cloud-only",
            BUSYBar.ConnectionWay.Cloud(cloudId)
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(cloudOnly),
            isNetworkAvailable = true,
            principal = BUSYLibUserPrincipal.Empty
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertTrue(
            setup.storage.devices.value.isEmpty(),
            "Logged-out user must not retain cloud-only devices"
        )
    }

    @Test
    fun GIVEN_empty_principal_with_multi_transport_device_THEN_only_cloud_stripped() = runTest {
        val cloudId = Uuid.random()
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = BUSYLibUserPrincipal.Empty
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val remaining = setup.storage.devices.value.single()
        assertEquals("device-1", remaining.uniqueId)
        assertNull(remaining.cloud)
        assertNotNull(remaining.ble)
    }

    @Test
    fun GIVEN_empty_principal_with_mixed_devices_THEN_all_cloud_links_removed_in_single_transaction() = runTest {
        val cloudOnlyId = Uuid.random()
        val multiTransportCloudId = Uuid.random()
        val cloudOnly = busyBar(
            id = "cloud-only",
            BUSYBar.ConnectionWay.Cloud(cloudOnlyId)
        )
        val multiTransport = busyBar(
            id = "multi-transport",
            BUSYBar.ConnectionWay.Cloud(multiTransportCloudId),
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )
        val bleOnly = busyBar(
            id = "ble-only",
            BUSYBar.ConnectionWay.BLE("DD:EE:FF")
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(cloudOnly, multiTransport, bleOnly),
            isNetworkAvailable = true,
            principal = BUSYLibUserPrincipal.Empty
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val remaining = setup.storage.devices.value
        assertEquals(2, remaining.size, "Cloud-only device must be removed; others retained")
        assertTrue(remaining.none { it.uniqueId == "cloud-only" })
        val multi = remaining.first { it.uniqueId == "multi-transport" }
        assertNull(multi.cloud, "Cloud transport must be stripped from multi-transport device")
        assertNotNull(multi.ble)
        val ble = remaining.first { it.uniqueId == "ble-only" }
        assertNull(ble.cloud)
        assertEquals(
            1,
            setup.storage.transactionCount,
            "All cloud-link removals must land in a single transaction"
        )
    }
}
