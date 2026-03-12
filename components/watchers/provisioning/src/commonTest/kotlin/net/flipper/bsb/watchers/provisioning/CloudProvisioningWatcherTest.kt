package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class CloudProvisioningWatcherTest {

    // region Test Cases

    @Test
    fun GIVEN_only_local_transport_WHEN_connected_to_cloud_THEN_adds_cloud_transport_to_device() =
        runTest {
            val cloudId = Uuid.random()
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )

            val setup = createSetup(
                devices = mutableListOf(device),
                connectedDevice = device,
                linkedInfo = RpcLinkedAccountInfo(linked = true, cloudId = cloudId.toString())
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val updated = setup.storage.findDevice("device-1")
            assertNotNull(updated)
            assertNotNull(updated.cloud)
            assertEquals(cloudId, updated.cloud!!.deviceId)
            assertNotNull(updated.ble)

            setup.cleanup()
        }

    @Test
    fun GIVEN_only_local_transport_WHEN_not_connected_to_cloud_THEN_no_changes() = runTest {
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )

        val setup = createSetup(
            devices = mutableListOf(device),
            connectedDevice = device,
            linkedInfo = RpcLinkedAccountInfo(linked = false, cloudId = null)
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.findDevice("device-1")
        assertNotNull(updated)
        assertEquals(device.connectionWays, updated.connectionWays)

        setup.cleanup()
    }

    @Test
    fun GIVEN_local_and_cloud_transport_WHEN_cloud_linked_to_current_device_THEN_no_changes() =
        runTest {
            val cloudId = Uuid.random()
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.Cloud(cloudId),
                BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )

            val setup = createSetup(
                devices = mutableListOf(device),
                connectedDevice = device,
                linkedInfo = RpcLinkedAccountInfo(linked = true, cloudId = cloudId.toString())
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val updated = setup.storage.findDevice("device-1")
            assertNotNull(updated)
            assertEquals(device.connectionWays, updated.connectionWays)

            setup.cleanup()
        }

    @Test
    fun GIVEN_local_and_cloud_WHEN_linked_to_different_device_THEN_creates_new_and_switches() =
        runTest {
            val originalCloudId = Uuid.random()
            val newCloudId = Uuid.random()
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.Cloud(originalCloudId),
                BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )

            val setup = createSetup(
                devices = mutableListOf(device),
                connectedDevice = device,
                linkedInfo = RpcLinkedAccountInfo(linked = true, cloudId = newCloudId.toString())
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            // Original device should still exist unchanged
            val original = setup.storage.findDevice("device-1")
            assertNotNull(original)
            assertEquals(device.connectionWays, original.connectionWays)

            // New device should be created with the new cloud connection
            val newDevice = setup.storage.devices.find { it.uniqueId != "device-1" }
            assertNotNull(newDevice, "New device should be created")
            assertNotNull(newDevice.cloud)
            assertEquals(newCloudId, newDevice.cloud!!.deviceId)

            // Current device should be switched to new device
            assertEquals(newDevice, setup.storage.currentDevice)

            setup.cleanup()
        }

    @Test
    fun GIVEN_local_and_cloud_WHEN_cloud_linked_to_different_device_and_existing_found_THEN_switches_to_existing() =
        runTest {
            val originalCloudId = Uuid.random()
            val targetCloudId = Uuid.random()
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.Cloud(originalCloudId),
                BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )
            val existingDevice = busyBar(
                id = "device-2",
                BUSYBar.ConnectionWay.Cloud(targetCloudId)
            )

            val setup = createSetup(
                devices = mutableListOf(device, existingDevice),
                connectedDevice = device,
                linkedInfo = RpcLinkedAccountInfo(
                    linked = true,
                    cloudId = targetCloudId.toString()
                )
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            // Should switch to existing device
            assertEquals(existingDevice, setup.storage.currentDevice)
            // No new devices created
            assertEquals(2, setup.storage.devices.size)

            setup.cleanup()
        }

    @Test
    fun GIVEN_local_and_cloud_WHEN_not_connected_to_cloud_THEN_removes_cloud_connection() =
        runTest {
            val cloudId = Uuid.random()
            val device = busyBar(
                id = "device-1",
                BUSYBar.ConnectionWay.Cloud(cloudId),
                BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )

            val setup = createSetup(
                devices = mutableListOf(device),
                connectedDevice = device,
                linkedInfo = RpcLinkedAccountInfo(linked = false, cloudId = null)
            )

            setup.watcher.onLaunch()
            advanceUntilIdle()

            val updated = setup.storage.findDevice("device-1")
            assertNotNull(updated)
            assertNull(updated.cloud, "Cloud connection should be removed")
            assertNotNull(updated.ble, "BLE connection should remain")

            setup.cleanup()
        }

    @Test
    fun GIVEN_connected_device_WHEN_linked_info_is_null_THEN_no_changes() = runTest {
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )

        val setup = createSetup(
            devices = mutableListOf(device),
            connectedDevice = device,
            linkedInfo = null
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.findDevice("device-1")
        assertNotNull(updated)
        assertEquals(device.connectionWays, updated.connectionWays)

        setup.cleanup()
    }

    @Test
    fun GIVEN_only_local_WHEN_connected_to_cloud_THEN_cloud_sorted_by_priority() = runTest {
        val cloudId = Uuid.random()
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Lan("10.0.4.20"),
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )

        val setup = createSetup(
            devices = mutableListOf(device),
            connectedDevice = device,
            linkedInfo = RpcLinkedAccountInfo(linked = true, cloudId = cloudId.toString())
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.findDevice("device-1")
        assertNotNull(updated)
        // All connection ways should be present
        assertNotNull(updated.lan)
        assertNotNull(updated.cloud)
        assertNotNull(updated.ble)
        // Priority order in connectionWays: Lan(100) > Cloud(10) > BLE(0)
        assertTrue(updated.connectionWays[0] is BUSYBar.ConnectionWay.Lan)
        assertTrue(updated.connectionWays[1] is BUSYBar.ConnectionWay.Cloud)
        assertTrue(updated.connectionWays[2] is BUSYBar.ConnectionWay.BLE)

        setup.cleanup()
    }

    // endregion

    // region Test Setup

    private data class TestSetup(
        val watcher: CloudProvisioningWatcher,
        val storage: FakePersistedStorage,
        val scope: CoroutineScope
    ) {
        fun cleanup() {
            scope.cancel()
        }
    }

    private fun TestScope.createSetup(
        devices: MutableList<BUSYBar>,
        connectedDevice: BUSYBar,
        linkedInfo: RpcLinkedAccountInfo?
    ): TestSetup {
        val storage = FakePersistedStorage(devices)

        val rpcApi = FakeRpcCriticalFeatureApi(
            accountInfoFlow = MutableStateFlow(linkedInfo)
        )
        val featureFlow = MutableStateFlow<FFeatureStatus<FRpcCriticalFeatureApi>>(
            FFeatureStatus.Supported(rpcApi)
        )

        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val orchestratorState = MutableStateFlow<FDeviceConnectStatus>(
            FDeviceConnectStatus.Connected(
                scope = scope,
                device = connectedDevice,
                deviceApi = FakeConnectedDeviceApi(),
                transportType = null
            )
        )

        val watcher = CloudProvisioningWatcher(
            scope = scope,
            featureProvider = FakeFeatureProvider(featureFlow),
            orchestrator = FakeOrchestrator(orchestratorState),
            persistedStorage = storage
        )

        return TestSetup(watcher, storage, scope)
    }

    private fun busyBar(id: String, vararg connectionWays: BUSYBar.ConnectionWay): BUSYBar {
        var ble: BUSYBar.ConnectionWay.BLE? = null
        var cloud: BUSYBar.ConnectionWay.Cloud? = null
        var lan: BUSYBar.ConnectionWay.Lan? = null
        var mock: BUSYBar.ConnectionWay.Mock? = null
        for (way in connectionWays) {
            when (way) {
                is BUSYBar.ConnectionWay.BLE -> ble = way
                is BUSYBar.ConnectionWay.Cloud -> cloud = way
                is BUSYBar.ConnectionWay.Lan -> lan = way
                is BUSYBar.ConnectionWay.Mock -> mock = way
            }
        }
        return BUSYBar(
            humanReadableName = "Test Bar",
            uniqueId = id,
            ble = ble,
            cloud = cloud,
            lan = lan,
            mock = mock
        )
    }

    // endregion

    // region Fakes

    private class FakePersistedStorage(
        val devices: MutableList<BUSYBar>
    ) : FDevicePersistedStorage {
        var currentDevice: BUSYBar? = null

        fun findDevice(id: String): BUSYBar? = devices.find { it.uniqueId == id }

        override fun getCurrentDeviceFlow() = flowOf(currentDevice).wrap()
        override fun getAllDevicesFlow() = flowOf(devices.toList()).wrap()

        override suspend fun <T> transaction(block: suspend PersistedStorageTransactionScope.() -> T): T {
            val scope = object : PersistedStorageTransactionScope {
                override fun getCurrentDevice() = this@FakePersistedStorage.currentDevice
                override fun getAllDevices() = this@FakePersistedStorage.devices.toList()

                override fun setCurrentDevice(device: BUSYBar?) {
                    this@FakePersistedStorage.currentDevice = device
                }

                override fun addOrReplace(device: BUSYBar) {
                    this@FakePersistedStorage.devices.removeAll { it.uniqueId == device.uniqueId }
                    this@FakePersistedStorage.devices.add(device)
                }

                override fun removeDevice(id: String) {
                    this@FakePersistedStorage.devices.removeAll { it.uniqueId == id }
                }
            }
            return scope.block()
        }
    }

    private class FakeOrchestrator(
        private val stateFlow: MutableStateFlow<FDeviceConnectStatus>
    ) : FDeviceOrchestrator {
        override fun getState(): WrappedStateFlow<FDeviceConnectStatus> = stateFlow.wrap()
        override suspend fun connectIfNot(config: BUSYBar) = Unit
        override suspend fun disconnectCurrent() = Unit
    }

    private class FakeFeatureProvider(
        private val rpcFlow: Flow<FFeatureStatus<FRpcCriticalFeatureApi>>
    ) : FFeatureProvider {
        @Suppress("UNCHECKED_CAST")
        override fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>> {
            return rpcFlow as Flow<FFeatureStatus<T>>
        }

        override suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T? = null
    }

    private class FakeRpcCriticalFeatureApi(
        accountInfoFlow: StateFlow<RpcLinkedAccountInfo?>
    ) : FRpcCriticalFeatureApi {
        override val currentAccountInfo: StateFlow<RpcLinkedAccountInfo?> = accountInfoFlow
        override val clientModeApi: FRpcClientModeApi
            get() = error("Not used in test")

        override suspend fun invalidateLinkedUser(userId: Uuid?) =
            error("Not used in test")

        override suspend fun getLinkCode(): Result<BusyBarLinkCode> =
            error("Not used in test")

        override suspend fun deleteAccount(): Result<SuccessResponse> =
            error("Not used in test")
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
