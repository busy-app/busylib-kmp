package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.feature.hardwareid.api.FHardwareIdFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bsb.watchers.provisioning.fakes.FakeCloudInvalidator
import net.flipper.bsb.watchers.provisioning.fakes.FakeConnectedDeviceApi
import net.flipper.bsb.watchers.provisioning.fakes.FakeFeatureProvider
import net.flipper.bsb.watchers.provisioning.fakes.FakeHardwareIdFeatureApi
import net.flipper.bsb.watchers.provisioning.fakes.FakeOrchestrator
import net.flipper.bsb.watchers.provisioning.fakes.FakePersistedStorage
import net.flipper.bsb.watchers.provisioning.fakes.HardwareIdProvisioningTestSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HardwareIdProvisioningWatcherTest {

    // region First provisioning: device has no hardwareId yet

    @Test
    fun GIVEN_null_hardwareId_WHEN_id_received_THEN_set_without_invalidation() =
        runTest {
            val device = busyBar(id = "device-1", BUSYBar.ConnectionWay.BLE("AA:BB:CC"))

            val setup = createSetup(
                devices = listOf(device),
                connectedDevice = device,
                hardwareIds = flowOf("HW-123")
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val updated = setup.storage.devices.value.single()
            assertEquals("device-1", updated.uniqueId)
            assertEquals("HW-123", updated.hardwareId)
            assertNotNull(updated.ble, "BLE transport must be preserved")
            assertEquals(0, setup.cloudInvalidator.invalidateCount, "First provisioning must not invalidate cloud")
        }

    @Test
    fun GIVEN_multi_transport_device_with_null_hardwareId_WHEN_id_received_THEN_all_transports_preserved() =
        runTest {
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.BLE("AA:BB:CC"),
                BUSYBar.ConnectionWay.Lan("10.0.4.20")
            )

            val setup = createSetup(
                devices = listOf(device),
                connectedDevice = device,
                hardwareIds = flowOf("HW-456")
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val updated = setup.storage.devices.value.single()
            assertEquals("HW-456", updated.hardwareId)
            assertNotNull(updated.ble, "BLE transport must be preserved")
            assertNotNull(updated.lan, "LAN transport must be preserved")
            assertEquals(0, setup.cloudInvalidator.invalidateCount)
        }

    // endregion

    // region No-op cases

    @Test
    fun GIVEN_device_with_matching_hardwareId_WHEN_same_id_received_THEN_no_change_and_no_invalidation() = runTest {
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC"),
            hardwareId = "HW-123"
        )

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = device,
            hardwareIds = flowOf("HW-123")
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertEquals("HW-123", updated.hardwareId, "Matching hardwareId must not be touched")
        assertNull(setup.storage.currentDevice, "Current device must not change when id matches")
        assertEquals(0, setup.cloudInvalidator.invalidateCount)
    }

    @Test
    fun GIVEN_device_with_null_hardwareId_WHEN_null_id_emitted_THEN_no_change() = runTest {
        val device = busyBar(id = "device-1", BUSYBar.ConnectionWay.BLE("AA:BB:CC"))

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = device,
            hardwareIds = flowOf(null)
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertNull(updated.hardwareId, "A null hardwareId emission must be ignored")
        assertEquals(0, setup.cloudInvalidator.invalidateCount)
    }

    @Test
    fun GIVEN_disconnected_WHEN_launched_THEN_no_change() = runTest {
        val device = busyBar(id = "device-1", BUSYBar.ConnectionWay.BLE("AA:BB:CC"))

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = null,
            hardwareIds = flowOf("HW-123")
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertNull(updated.hardwareId, "hardwareId must remain null while disconnected")
        assertEquals(0, setup.cloudInvalidator.invalidateCount)
    }

    @Test
    fun GIVEN_feature_unsupported_WHEN_connected_THEN_no_change() = runTest {
        assertFeatureStatusIsIgnored(FFeatureStatus.Unsupported)
    }

    @Test
    fun GIVEN_feature_not_found_WHEN_connected_THEN_no_change() = runTest {
        assertFeatureStatusIsIgnored(FFeatureStatus.NotFound)
    }

    @Test
    fun GIVEN_feature_retrieving_WHEN_connected_THEN_no_change() = runTest {
        assertFeatureStatusIsIgnored(FFeatureStatus.Retrieving)
    }

    // endregion

    // region Hardware id changed: re-provisioning + cloud invalidation

    @Test
    fun GIVEN_changed_hardwareId_and_no_match_WHEN_id_received_THEN_new_current_and_invalidation() =
        runTest {
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.BLE("AA:BB:CC"),
                hardwareId = "old-hw"
            )

            val setup = createSetup(
                devices = listOf(device),
                connectedDevice = device,
                hardwareIds = flowOf("new-hw")
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val current = assertNotNull(setup.storage.currentDevice, "A new device must become current")
            assertEquals("new-hw", current.hardwareId)
            assertNotNull(current.ble, "BLE transport must carry over to the new device")
            assertNull(current.cloud, "Cloud link must be dropped on the re-provisioned device")
            assertEquals("device-1", setup.storage.devices.value.first { it.uniqueId == "device-1" }.uniqueId)
            assertEquals(1, setup.cloudInvalidator.invalidateCount, "Changed hardwareId must invalidate cloud once")
        }

    @Test
    fun GIVEN_changed_hardwareId_with_match_WHEN_id_received_THEN_existing_current_and_invalidation() =
        runTest {
            val connected = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.BLE("AA:BB:CC"),
                hardwareId = "old-hw"
            )
            val existing = busyBar(
                id = "device-2",
                BUSYBar.ConnectionWay.BLE("DD:EE:FF"),
                hardwareId = "new-hw"
            )

            val setup = createSetup(
                devices = listOf(connected, existing),
                connectedDevice = connected,
                hardwareIds = flowOf("new-hw")
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val current = assertNotNull(setup.storage.currentDevice)
            assertEquals(
                "device-2",
                current.uniqueId,
                "The pre-existing device with the new hardwareId must be selected"
            )
            assertEquals(
                2,
                setup.storage.devices.value.size,
                "No extra device must be created when one already matches"
            )
            assertEquals(1, setup.cloudInvalidator.invalidateCount)
        }

    @Test
    fun GIVEN_status_request_in_flight_WHEN_device_disconnects_THEN_late_status_is_ignored() =
        runTest {
            // The pre-PR pipeline cancelled an in-flight device-status request when the device
            // dropped. The filter rework must keep that guarantee: a status that resolves after
            // disconnect belongs to a device that is no longer connected and must not be applied.
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )
            val storage = FakePersistedStorage(MutableStateFlow(listOf(device)))
            val gate = CompletableDeferred<Unit>()
            val rpcApi = FakeRpcFeatureApi(
                systemApi = FakeRpcSystemApi(
                    deviceStatusResult = Result.success(fakeDeviceStatus(serialNumber = "SN-late")),
                    awaitBeforeResult = { gate.await() }
                )
            )
            val featureFlow = MutableStateFlow<FFeatureStatus<FRpcFeatureApi>>(
                FFeatureStatus.Supported(rpcApi)
            )
            val scope = CoroutineScope(
                SupervisorJob(backgroundScope.coroutineContext.job) + StandardTestDispatcher(testScheduler)
            )
            val orchestratorState = MutableStateFlow<FDeviceConnectStatus>(
                FDeviceConnectStatus.Connected(
                    scope = scope,
                    device = device,
                    deviceApi = FakeConnectedDeviceApi(),
                    transportType = FDeviceTransportType.BLE
                )
            )
            val watcher = HardwareIdProvisioningWatcher(
                scope = scope,
                featureProvider = FakeFeatureProvider(featureFlow),
                orchestrator = FakeOrchestrator(orchestratorState),
                persistedStorage = storage
            )

            watcher.onLaunch()
            advanceUntilIdle()

            // Device drops before the status request resolves.
            orchestratorState.value = FDeviceConnectStatus.Disconnected(
                device = device,
                reason = DisconnectStatus.REPORTED_BY_TRANSPORT
            )
            advanceUntilIdle()

            // The status request now resolves, but its device is already gone.
            gate.complete(Unit)
            advanceUntilIdle()

            assertNull(
                storage.devices.value.single().hardwareId,
                "A device status that resolves after disconnect must not be applied"
            )
        }

    // endregion

    // region Test Setup

    private fun TestScope.assertFeatureStatusIsIgnored(featureStatus: FFeatureStatus<FHardwareIdFeatureApi>) {
        val device = busyBar(id = "device-1", BUSYBar.ConnectionWay.BLE("AA:BB:CC"))

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = device,
            hardwareIds = flowOf("HW-123"),
            featureStatus = featureStatus
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertNull(updated.hardwareId, "hardwareId must remain null when feature is not supported")
        assertEquals(0, setup.cloudInvalidator.invalidateCount)
    }

    private fun TestScope.createSetup(
        devices: List<BUSYBar>,
        connectedDevice: BUSYBar?,
        hardwareIds: Flow<String?>,
        featureStatus: FFeatureStatus<FHardwareIdFeatureApi>? = null
    ): HardwareIdProvisioningTestSetup {
        val storage = FakePersistedStorage(MutableStateFlow(devices))
        val cloudInvalidator = FakeCloudInvalidator()

        val featureFlow = MutableStateFlow(
            featureStatus ?: FFeatureStatus.Supported(FakeHardwareIdFeatureApi(hardwareIds))
        )

        val scope = CoroutineScope(
            SupervisorJob(backgroundScope.coroutineContext.job) + StandardTestDispatcher(testScheduler)
        )

        val orchestratorState = MutableStateFlow<FDeviceConnectStatus>(
            if (connectedDevice != null) {
                FDeviceConnectStatus.Connected(
                    scope = scope,
                    device = connectedDevice,
                    deviceApi = FakeConnectedDeviceApi(),
                    transportType = FDeviceTransportType.BLE
                )
            } else {
                FDeviceConnectStatus.Disconnected(
                    device = null,
                    reason = DisconnectStatus.NOT_INITIALIZED
                )
            }
        )

        val watcher = HardwareIdProvisioningWatcher(
            scope = scope,
            featureProvider = FakeFeatureProvider(featureFlow),
            orchestrator = FakeOrchestrator(orchestratorState),
            persistedStorage = storage,
            cloudInvalidator = cloudInvalidator
        )

        return HardwareIdProvisioningTestSetup(watcher, storage, cloudInvalidator)
    }

    private fun busyBar(
        id: String,
        vararg connectionWays: BUSYBar.ConnectionWay,
        hardwareId: String? = null
    ): BUSYBar {
        require(connectionWays.isNotEmpty()) { "At least one connection way is required" }
        val first = connectionWays.first()
        var result = when (first) {
            is BUSYBar.ConnectionWay.BLE -> BUSYBar(
                humanReadableName = "Test Bar",
                uniqueId = id,
                ble = first
            ).copy(hardwareId = hardwareId)
            is BUSYBar.ConnectionWay.Cloud -> BUSYBar(
                humanReadableName = "Test Bar",
                hardwareId = hardwareId,
                uniqueId = id,
                cloud = first
            )
            is BUSYBar.ConnectionWay.Lan -> BUSYBar(
                humanReadableName = "Test Bar",
                uniqueId = id,
                lan = first
            ).copy(hardwareId = hardwareId)
            is BUSYBar.ConnectionWay.Mock -> BUSYBar(
                humanReadableName = "Test Bar",
                hardwareId = hardwareId,
                uniqueId = id,
                mock = first
            )
        }
        for (way in connectionWays.drop(1)) {
            result = when (way) {
                is BUSYBar.ConnectionWay.BLE -> result.addTransport(ble = way)
                is BUSYBar.ConnectionWay.Cloud -> result.addTransport(cloud = way)
                is BUSYBar.ConnectionWay.Lan -> result.addTransport(lan = way)
                is BUSYBar.ConnectionWay.Mock -> result.addTransport(mock = way)
            }
        }
        return result
    }

    // endregion
}
