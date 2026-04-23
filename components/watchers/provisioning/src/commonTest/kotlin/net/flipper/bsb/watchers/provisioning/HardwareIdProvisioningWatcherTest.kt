package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusDevice
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bsb.watchers.provisioning.fakes.FakeConnectedDeviceApi
import net.flipper.bsb.watchers.provisioning.fakes.FakeFeatureProvider
import net.flipper.bsb.watchers.provisioning.fakes.FakeOrchestrator
import net.flipper.bsb.watchers.provisioning.fakes.FakePersistedStorage
import net.flipper.bsb.watchers.provisioning.fakes.FakeRpcFeatureApi
import net.flipper.bsb.watchers.provisioning.fakes.FakeRpcSystemApi
import net.flipper.bsb.watchers.provisioning.fakes.HardwareIdProvisioningTestSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HardwareIdProvisioningWatcherTest {

    // region Test Cases

    @Test
    fun GIVEN_device_with_null_hardwareId_WHEN_connected_and_status_received_THEN_hardwareId_updated() =
        runTest {
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )

            val setup = createSetup(
                devices = listOf(device),
                connectedDevice = device,
                deviceStatusResult = Result.success(fakeDeviceStatus(serialNumber = "SN-123"))
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val updated = setup.storage.devices.value.single()
            assertEquals("device-1", updated.uniqueId)
            assertEquals("SN-123", updated.hardwareId)
            assertNotNull(updated.ble, "BLE transport must be preserved")
        }

    @Test
    fun GIVEN_device_with_existing_hardwareId_WHEN_connected_THEN_no_changes() = runTest {
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC"),
            hardwareId = "existing-hw"
        )

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = device,
            deviceStatusResult = Result.success(fakeDeviceStatus(serialNumber = "SN-123"))
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertEquals("existing-hw", updated.hardwareId, "Existing hardwareId must not be overwritten")
    }

    @Test
    fun GIVEN_device_with_null_hardwareId_WHEN_getDeviceStatus_fails_THEN_no_changes() = runTest {
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = device,
            deviceStatusResult = Result.failure(IllegalStateException("RPC error"))
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertNull(updated.hardwareId, "hardwareId must remain null on failure")
    }

    @Test
    fun GIVEN_device_with_null_hardwareId_WHEN_feature_unsupported_THEN_no_changes() = runTest {
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = device,
            deviceStatusResult = Result.success(fakeDeviceStatus(serialNumber = "SN-123")),
            featureStatus = FFeatureStatus.Unsupported
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertNull(updated.hardwareId, "hardwareId must remain null when feature is unsupported")
    }

    @Test
    fun GIVEN_disconnected_WHEN_launched_THEN_no_changes() = runTest {
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )

        val setup = createSetup(
            devices = listOf(device),
            connectedDevice = null,
            deviceStatusResult = Result.success(fakeDeviceStatus(serialNumber = "SN-123"))
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertNull(updated.hardwareId, "hardwareId must remain null when disconnected")
    }

    @Test
    fun GIVEN_multi_transport_device_WHEN_status_received_THEN_all_transports_preserved() =
        runTest {
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.BLE("AA:BB:CC"),
                BUSYBar.ConnectionWay.Lan("10.0.4.20")
            )

            val setup = createSetup(
                devices = listOf(device),
                connectedDevice = device,
                deviceStatusResult = Result.success(fakeDeviceStatus(serialNumber = "SN-456"))
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val updated = setup.storage.devices.value.single()
            assertEquals("SN-456", updated.hardwareId)
            assertNotNull(updated.ble, "BLE transport must be preserved")
            assertNotNull(updated.lan, "LAN transport must be preserved")
        }

    // endregion

    // region Test Setup

    private fun TestScope.createSetup(
        devices: List<BUSYBar>,
        connectedDevice: BUSYBar?,
        deviceStatusResult: Result<BusyBarStatusDevice>,
        featureStatus: FFeatureStatus<FRpcFeatureApi>? = null
    ): HardwareIdProvisioningTestSetup {
        val storage = FakePersistedStorage(MutableStateFlow(devices))

        val rpcApi = FakeRpcFeatureApi(
            systemApi = FakeRpcSystemApi(deviceStatusResult)
        )
        val featureFlow = MutableStateFlow(
            featureStatus ?: FFeatureStatus.Supported(rpcApi)
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
            persistedStorage = storage
        )

        return HardwareIdProvisioningTestSetup(watcher, storage)
    }

    private fun fakeDeviceStatus(serialNumber: String) = BusyBarStatusDevice(
        serialNumber = serialNumber,
        usbMac = "00:00:00:00:00:00",
        wifiMac = "00:00:00:00:00:00",
        bleMac = "00:00:00:00:00:00",
        otpValid = true,
        otpModel = "test-model",
        otpTimestamp = Instant.fromEpochSeconds(0)
    )

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
