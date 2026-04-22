package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatus
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusDevice
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.feature.rpc.api.model.StatusFirmware
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import kotlin.reflect.KClass
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

    private data class TestSetup(
        val watcher: HardwareIdProvisioningWatcher,
        val storage: FakePersistedStorage
    )

    private fun TestScope.createSetup(
        devices: List<BUSYBar>,
        connectedDevice: BUSYBar?,
        deviceStatusResult: Result<BusyBarStatusDevice>,
        featureStatus: FFeatureStatus<FRpcFeatureApi>? = null
    ): TestSetup {
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

        return TestSetup(watcher, storage)
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
                hardwareId = hardwareId,
                uniqueId = id,
                ble = first
            )
            is BUSYBar.ConnectionWay.Cloud -> BUSYBar(
                humanReadableName = "Test Bar",
                hardwareId = hardwareId,
                uniqueId = id,
                cloud = first
            )
            is BUSYBar.ConnectionWay.Lan -> BUSYBar(
                humanReadableName = "Test Bar",
                hardwareId = hardwareId,
                uniqueId = id,
                lan = first
            )
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

    // region Fakes

    private class FakePersistedStorage(
        val devices: MutableStateFlow<List<BUSYBar>>
    ) : FInternalDevicePersistedStorage {
        private val currentDeviceFlow = MutableStateFlow<BUSYBar?>(null)

        override fun getCurrentDeviceFlow() = currentDeviceFlow.asFlow().wrap()
        override fun getAllDevicesFlow() = devices.asFlow().wrap()

        override suspend fun addHook(vararg hook: TransactionHook) = Unit

        override suspend fun <T> transactionInternal(block: suspend InternalStorageTransactionScope.() -> T): T {
            val scope = object : InternalStorageTransactionScope {
                override fun getCurrentDevice() = this@FakePersistedStorage.currentDeviceFlow.value
                override fun getAllDevices() = this@FakePersistedStorage.devices.value.toList()

                override fun setCurrentDevice(device: BUSYBar) {
                    this@FakePersistedStorage.currentDeviceFlow.value = device
                }

                override fun setCurrentDeviceNullable(device: BUSYBar?) {
                    this@FakePersistedStorage.currentDeviceFlow.value = device
                }

                override fun addOrReplace(device: BUSYBar) {
                    this@FakePersistedStorage.devices.update { list ->
                        list.filter { it.uniqueId != device.uniqueId } + device
                    }
                }

                override fun removeDevice(id: String) {
                    this@FakePersistedStorage.devices.update { list ->
                        list.filter { it.uniqueId != id }
                    }
                    if (this@FakePersistedStorage.currentDeviceFlow.value?.uniqueId == id) {
                        this@FakePersistedStorage.currentDeviceFlow.value = null
                    }
                }
            }
            return scope.block()
        }

        override suspend fun <T> transaction(block: suspend PersistedStorageTransactionScope.() -> T): T {
            return transactionInternal { block() }
        }
    }

    private class FakeOrchestrator(
        private val stateFlow: MutableStateFlow<FDeviceConnectStatus>
    ) : FDeviceOrchestrator {
        override fun getState(): WrappedStateFlow<FDeviceConnectStatus> = stateFlow.wrap()
    }

    private class FakeFeatureProvider(
        private val rpcFlow: Flow<FFeatureStatus<FRpcFeatureApi>>
    ) : FFeatureProvider {
        @Suppress("UNCHECKED_CAST")
        override fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>> {
            return rpcFlow as Flow<FFeatureStatus<T>>
        }

        override suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T? = null
    }

    private class FakeRpcFeatureApi(
        private val systemApi: FRpcSystemApi
    ) : FRpcFeatureApi {
        override val fRpcSystemApi: FRpcSystemApi get() = systemApi
        override val fRpcWifiApi: FRpcWifiApi get() = error("Not used in test")
        override val fRpcBleApi: FRpcBleApi get() = error("Not used in test")
        override val fRpcSettingsApi: FRpcSettingsApi get() = error("Not used in test")
        override val fRpcStreamingApi: FRpcStreamingApi get() = error("Not used in test")
        override val fRpcAssetsApi: FRpcAssetsApi get() = error("Not used in test")
        override val fRpcUpdaterApi: FRpcUpdaterApi get() = error("Not used in test")
        override val fRpcMatterApi: FRpcMatterApi get() = error("Not used in test")
        override val fRpcTimeZoneApi: FRpcTimeZoneApi get() = error("Not used in test")
    }

    private class FakeRpcSystemApi(
        private val deviceStatusResult: Result<BusyBarStatusDevice>
    ) : FRpcSystemApi {
        override suspend fun getVersion(): Result<BusyBarVersion> = error("Not used in test")
        override suspend fun getStatus(): Result<BusyBarStatus> = error("Not used in test")
        override suspend fun getDeviceStatus(): Result<BusyBarStatusDevice> = deviceStatusResult
        override suspend fun getStatusFirmware(): Result<StatusFirmware> = error("Not used in test")
        override suspend fun getStatusSystem(): Result<BusyBarStatusSystem> = error("Not used in test")
        override suspend fun getStatusPower(): Result<BusyBarStatusPower> = error("Not used in test")
    }

    private class FakeConnectedDeviceApi : FConnectedDeviceApi {
        override val deviceName = "Test Device"
        override suspend fun tryUpdateConnectionConfig(
            config: FDeviceConnectionConfig<*>
        ): Result<Unit> = Result.failure(NotImplementedError())

        override suspend fun disconnect() = Unit
    }

    // endregion
}
