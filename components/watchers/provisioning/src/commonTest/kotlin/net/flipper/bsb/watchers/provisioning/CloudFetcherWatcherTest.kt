package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.watchers.provisioning.fakes.DEFAULT_BUSY_BAR_NAME
import net.flipper.bsb.watchers.provisioning.fakes.DEFAULT_TEST_BAR_NAME
import net.flipper.bsb.watchers.provisioning.fakes.busyBar
import net.flipper.bsb.watchers.provisioning.fakes.cloudBar
import net.flipper.bsb.watchers.provisioning.fakes.createCloudFetcherSetup
import net.flipper.bsb.watchers.provisioning.fakes.fakeToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class CloudFetcherWatcherTest {

    @Test
    fun GIVEN_loading_principal_THEN_no_storage_mutation() = runTest {
        val cloudOnly = busyBar(
            id = "cloud-only",
            BUSYBar.ConnectionWay.Cloud(Uuid.random())
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(cloudOnly),
            isNetworkAvailable = true,
            principal = BUSYLibUserPrincipal.Loading
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(cloudOnly), setup.storage.devices.value)
        assertEquals(0, setup.storage.transactionCount)
        assertEquals(
            0,
            setup.cloudBarsApi.callCount,
            "Loading principal must not trigger cloud fetch"
        )
    }

    @Test
    fun GIVEN_token_principal_without_network_THEN_no_storage_mutation() = runTest {
        val cloudOnly = busyBar(
            id = "cloud-only",
            BUSYBar.ConnectionWay.Cloud(Uuid.random())
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(cloudOnly),
            isNetworkAvailable = false,
            principal = fakeToken()
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(cloudOnly), setup.storage.devices.value)
        assertEquals(0, setup.storage.transactionCount)
        assertEquals(
            0,
            setup.cloudBarsApi.callCount,
            "Offline network must not trigger cloud fetch"
        )
    }

    @Test
    fun GIVEN_token_principal_and_api_failure_THEN_no_storage_mutation() = runTest {
        val cloudOnly = busyBar(
            id = "cloud-only",
            BUSYBar.ConnectionWay.Cloud(Uuid.random())
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(cloudOnly),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.failure(IllegalStateException("boom"))
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(cloudOnly), setup.storage.devices.value)
        assertEquals(0, setup.storage.transactionCount)
        assertEquals(1, setup.cloudBarsApi.callCount)
    }

    @Test
    fun GIVEN_token_principal_and_matching_bars_THEN_no_storage_mutation() = runTest {
        val cloudId = Uuid.random()
        val hardwareId = "hw-$cloudId"
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            hardwareId = hardwareId
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = DEFAULT_TEST_BAR_NAME, hardwareId = hardwareId))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(device), setup.storage.devices.value)
        assertEquals(1, setup.storage.transactionCount)
    }

    @Test
    fun GIVEN_matching_bars_in_different_order_THEN_no_storage_mutation() = runTest {
        val cloudIdA = Uuid.random()
        val cloudIdB = Uuid.random()
        val hardwareIdA = "hw-$cloudIdA"
        val hardwareIdB = "hw-$cloudIdB"
        val deviceA = busyBar(
            id = "device-A",
            BUSYBar.ConnectionWay.Cloud(cloudIdA),
            hardwareId = hardwareIdA
        )
        val deviceB = busyBar(
            id = "device-B",
            BUSYBar.ConnectionWay.Cloud(cloudIdB),
            hardwareId = hardwareIdB
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(deviceA, deviceB),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(
                    // Intentionally reversed order to verify sorted comparison
                    cloudBar(cloudIdB, label = DEFAULT_TEST_BAR_NAME, hardwareId = hardwareIdB),
                    cloudBar(cloudIdA, label = DEFAULT_TEST_BAR_NAME, hardwareId = hardwareIdA)
                )
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(
            1,
            setup.storage.transactionCount,
            "Equal sets differing only in order must not trigger invalidation"
        )
    }

    @Test
    fun GIVEN_new_cloud_bar_THEN_new_device_added_with_label() = runTest {
        val cloudId = Uuid.random()
        val setup = createCloudFetcherSetup(
            devices = emptyList(),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "My Bar"))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val created = setup.storage.devices.value.single()
        assertEquals("My Bar", created.humanReadableName)
        assertEquals(cloudId, created.cloud?.deviceId)
        assertNull(created.ble)
        assertNull(created.lan)
        assertNull(created.mock)
    }

    @Test
    fun GIVEN_new_cloud_bar_without_label_THEN_new_device_uses_default_name() = runTest {
        val cloudId = Uuid.random()
        val setup = createCloudFetcherSetup(
            devices = emptyList(),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = null))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val created = setup.storage.devices.value.single()
        assertEquals("BUSY Bar", created.humanReadableName)
        assertEquals(cloudId, created.cloud?.deviceId)
    }

    @Test
    fun GIVEN_orphaned_cloud_only_device_THEN_device_removed() = runTest {
        val orphanCloudId = Uuid.random()
        val freshCloudId = Uuid.random()
        val orphan = busyBar(
            id = "orphan",
            BUSYBar.ConnectionWay.Cloud(orphanCloudId)
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(orphan),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(freshCloudId, label = "Fresh"))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val remaining = setup.storage.devices.value
        assertEquals(1, remaining.size, "Orphan should be removed; fresh bar added")
        val fresh = remaining.single()
        assertEquals(freshCloudId, fresh.cloud?.deviceId)
        assertEquals("Fresh", fresh.humanReadableName)
    }

    @Test
    fun GIVEN_orphaned_device_with_ble_THEN_only_cloud_stripped() = runTest {
        val orphanCloudId = Uuid.random()
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(orphanCloudId),
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(emptyList())
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val remaining = setup.storage.devices.value.single()
        assertEquals("device-1", remaining.uniqueId)
        assertNull(remaining.cloud)
        assertNotNull(remaining.ble)
    }

    @Test
    fun GIVEN_mixed_bars_THEN_adds_new_and_removes_orphaned_in_single_transaction() = runTest {
        val keptCloudId = Uuid.random()
        val orphanCloudId = Uuid.random()
        val newCloudId = Uuid.random()

        val kept = busyBar(
            id = "kept",
            BUSYBar.ConnectionWay.Cloud(keptCloudId)
        )
        val orphan = busyBar(
            id = "orphan",
            BUSYBar.ConnectionWay.Cloud(orphanCloudId)
        )

        val setup = createCloudFetcherSetup(
            devices = listOf(kept, orphan),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(
                    cloudBar(keptCloudId, label = "Kept"),
                    cloudBar(newCloudId, label = "New")
                )
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val remaining = setup.storage.devices.value
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.uniqueId == "kept" && it.cloud?.deviceId == keptCloudId })
        assertTrue(remaining.none { it.uniqueId == "orphan" })
        val created = remaining.firstOrNull { it.cloud?.deviceId == newCloudId }
        assertNotNull(created)
        assertEquals("New", created.humanReadableName)
        assertEquals(
            1,
            setup.storage.transactionCount,
            "All changes must land in a single transaction"
        )
    }

    @Test
    fun GIVEN_principal_transitions_from_loading_to_token_THEN_sync_runs() = runTest {
        val cloudId = Uuid.random()
        val principalFlow = MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Loading)
        val setup = createCloudFetcherSetup(
            devices = emptyList(),
            isNetworkAvailable = true,
            principalFlow = principalFlow,
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "Fresh"))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertTrue(setup.storage.devices.value.isEmpty(), "Loading must not trigger sync")
        assertEquals(0, setup.cloudBarsApi.callCount, "Loading must not trigger cloud fetch")

        principalFlow.value = fakeToken()
        advanceUntilIdle()

        val created = setup.storage.devices.value.single()
        assertEquals(cloudId, created.cloud?.deviceId)
        assertTrue(setup.cloudBarsApi.callCount > 0, "Token principal must trigger cloud fetch")
    }

    @Test
    fun GIVEN_network_transitions_from_offline_to_online_THEN_sync_runs() = runTest {
        val cloudId = Uuid.random()
        val networkFlow = MutableStateFlow(false)
        val setup = createCloudFetcherSetup(
            devices = emptyList(),
            networkFlow = networkFlow,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "Fresh"))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertTrue(setup.storage.devices.value.isEmpty(), "Offline must not trigger sync")
        assertEquals(0, setup.cloudBarsApi.callCount, "Offline must not trigger cloud fetch")

        networkFlow.value = true
        advanceUntilIdle()

        val created = setup.storage.devices.value.single()
        assertEquals(cloudId, created.cloud?.deviceId)
        assertTrue(setup.cloudBarsApi.callCount > 0, "Online state must trigger cloud fetch")
    }

    @Test
    fun GIVEN_token_principal_and_empty_cloud_bars_with_local_cloud_THEN_local_cloud_removed() =
        runTest {
            val orphanCloudId = Uuid.random()
            val orphan = busyBar(
                id = "orphan",
                BUSYBar.ConnectionWay.Cloud(orphanCloudId)
            )
            val setup = createCloudFetcherSetup(
                devices = listOf(orphan),
                isNetworkAvailable = true,
                principal = fakeToken(),
                cloudBarsResult = Result.success(emptyList())
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            assertTrue(setup.storage.devices.value.isEmpty())
        }

    @Test
    fun GIVEN_non_cloud_device_with_matching_bars_THEN_untouched() = runTest {
        val bleOnly = busyBar(
            id = "ble-only",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )
        val cloudId = Uuid.random()
        val hardwareId = "hw-$cloudId"
        val cloudDevice = busyBar(
            id = "cloud-device",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            hardwareId = hardwareId
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(bleOnly, cloudDevice),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = DEFAULT_TEST_BAR_NAME, hardwareId = hardwareId))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(1, setup.storage.transactionCount)
        assertEquals(listOf(bleOnly, cloudDevice), setup.storage.devices.value)
    }

    @Test
    fun GIVEN_matching_ids_but_different_label_THEN_device_renamed() = runTest {
        val cloudId = Uuid.random()
        val hardwareId = "hw-$cloudId"
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            hardwareId = hardwareId,
            humanReadableName = DEFAULT_BUSY_BAR_NAME
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "Renamed", hardwareId = hardwareId))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertEquals("device-1", updated.uniqueId)
        assertEquals("Renamed", updated.humanReadableName)
        assertEquals(cloudId, updated.cloud?.deviceId)
        assertEquals(hardwareId, updated.hardwareId, "Existing hardwareId must be preserved")
    }

    @Test
    fun GIVEN_null_local_hardware_id_and_label_change_THEN_both_applied_in_single_transaction() = runTest {
        val cloudId = Uuid.random()
        val hardwareId = "hw-$cloudId"
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            humanReadableName = DEFAULT_BUSY_BAR_NAME
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "Renamed", hardwareId = hardwareId))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertEquals("Renamed", updated.humanReadableName)
        assertEquals(hardwareId, updated.hardwareId)
        assertEquals(1, setup.storage.transactionCount, "Name + hardwareId update must share one transaction")
    }

    @Test
    fun GIVEN_non_null_local_hardware_id_differing_from_cloud_THEN_local_preserved() = runTest {
        val cloudId = Uuid.random()
        val localHardwareId = "local-hw"
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            hardwareId = localHardwareId
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(
                    cloudBar(cloudId, label = DEFAULT_TEST_BAR_NAME, hardwareId = "cloud-hw")
                )
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(device), setup.storage.devices.value)
        assertEquals(
            1,
            setup.storage.transactionCount,
            "Differing but non-null local hardwareId must not be overwritten"
        )
    }

    @Test
    fun GIVEN_matching_ids_and_null_label_THEN_local_name_preserved() = runTest {
        val cloudId = Uuid.random()
        val hardwareId = "hw-$cloudId"
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            hardwareId = hardwareId
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = null, hardwareId = hardwareId))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(device), setup.storage.devices.value)
        assertEquals(
            1,
            setup.storage.transactionCount,
            "Null label must not overwrite the local humanReadableName"
        )
    }

    @Test
    fun GIVEN_multi_transport_device_with_label_change_THEN_only_name_updated() = runTest {
        val cloudId = Uuid.random()
        val hardwareId = "hw-$cloudId"
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            BUSYBar.ConnectionWay.BLE("AA:BB:CC"),
            hardwareId = hardwareId,
            humanReadableName = DEFAULT_BUSY_BAR_NAME
        )
        val setup = createCloudFetcherSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "Renamed", hardwareId = hardwareId))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertEquals("device-1", updated.uniqueId)
        assertEquals("Renamed", updated.humanReadableName)
        assertEquals(cloudId, updated.cloud?.deviceId)
        assertEquals(device.ble, updated.ble, "BLE transport must be preserved")
        assertEquals(hardwareId, updated.hardwareId, "Existing hardwareId must be preserved")
    }
}
