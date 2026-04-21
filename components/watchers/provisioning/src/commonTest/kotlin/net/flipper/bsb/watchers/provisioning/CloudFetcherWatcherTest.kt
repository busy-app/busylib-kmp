package net.flipper.bsb.watchers.provisioning

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketApi
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import net.flipper.bsb.watchers.provisioning.utils.CloudFetcher
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private const val DEFAULT_TEST_BAR_NAME = "Test Bar"

@OptIn(ExperimentalCoroutinesApi::class)
class CloudFetcherWatcherTest {

    // region Test Cases

    @Test
    fun GIVEN_empty_principal_THEN_no_storage_mutation() = runTest {
        val bleDevice = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )
        val setup = createSetup(
            devices = listOf(bleDevice),
            isNetworkAvailable = true,
            principal = BUSYLibUserPrincipal.Empty
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(bleDevice), setup.storage.devices.value)
        assertEquals(0, setup.storage.transactionCount)
    }

    @Test
    fun GIVEN_loading_principal_THEN_no_storage_mutation() = runTest {
        val cloudOnly = busyBar(
            id = "cloud-only",
            BUSYBar.ConnectionWay.Cloud(Uuid.random())
        )
        val setup = createSetup(
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
        val setup = createSetup(
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
        val setup = createSetup(
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
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId)
        )
        val setup = createSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = DEFAULT_TEST_BAR_NAME))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(device), setup.storage.devices.value)
        assertEquals(0, setup.storage.transactionCount)
    }

    @Test
    fun GIVEN_matching_bars_in_different_order_THEN_no_storage_mutation() = runTest {
        val cloudIdA = Uuid.random()
        val cloudIdB = Uuid.random()
        val deviceA = busyBar(
            id = "device-A",
            BUSYBar.ConnectionWay.Cloud(cloudIdA)
        )
        val deviceB = busyBar(
            id = "device-B",
            BUSYBar.ConnectionWay.Cloud(cloudIdB)
        )
        val setup = createSetup(
            devices = listOf(deviceA, deviceB),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(
                    // Intentionally reversed order to verify sorted comparison
                    cloudBar(cloudIdB, label = DEFAULT_TEST_BAR_NAME),
                    cloudBar(cloudIdA, label = DEFAULT_TEST_BAR_NAME)
                )
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(
            0,
            setup.storage.transactionCount,
            "Equal sets differing only in order must not trigger invalidation"
        )
    }

    @Test
    fun GIVEN_new_cloud_bar_THEN_new_device_added_with_label() = runTest {
        val cloudId = Uuid.random()
        val setup = createSetup(
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
        val setup = createSetup(
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
        val setup = createSetup(
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
        val setup = createSetup(
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

        val setup = createSetup(
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
        val setup = createSetup(
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
        val setup = createSetup(
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
            val setup = createSetup(
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
        val cloudDevice = busyBar(
            id = "cloud-device",
            BUSYBar.ConnectionWay.Cloud(cloudId)
        )
        val setup = createSetup(
            devices = listOf(bleOnly, cloudDevice),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = DEFAULT_TEST_BAR_NAME))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(0, setup.storage.transactionCount)
        assertEquals(listOf(bleOnly, cloudDevice), setup.storage.devices.value)
    }

    @Test
    fun GIVEN_matching_ids_but_different_label_THEN_device_renamed() = runTest {
        val cloudId = Uuid.random()
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId)
        )
        val setup = createSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "Renamed"))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertEquals("device-1", updated.uniqueId)
        assertEquals("Renamed", updated.humanReadableName)
        assertEquals(cloudId, updated.cloud?.deviceId)
    }

    @Test
    fun GIVEN_matching_ids_and_null_label_THEN_local_name_preserved() = runTest {
        val cloudId = Uuid.random()
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId)
        )
        val setup = createSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = null))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        assertEquals(listOf(device), setup.storage.devices.value)
        assertEquals(
            0,
            setup.storage.transactionCount,
            "Null label must not overwrite the local humanReadableName"
        )
    }

    @Test
    fun GIVEN_multi_transport_device_with_label_change_THEN_only_name_updated() = runTest {
        val cloudId = Uuid.random()
        val device = busyBar(
            id = "device-1",
            BUSYBar.ConnectionWay.Cloud(cloudId),
            BUSYBar.ConnectionWay.BLE("AA:BB:CC")
        )
        val setup = createSetup(
            devices = listOf(device),
            isNetworkAvailable = true,
            principal = fakeToken(),
            cloudBarsResult = Result.success(
                listOf(cloudBar(cloudId, label = "Renamed"))
            )
        )

        setup.watcher.onLaunch()
        advanceUntilIdle()

        val updated = setup.storage.devices.value.single()
        assertEquals("device-1", updated.uniqueId)
        assertEquals("Renamed", updated.humanReadableName)
        assertEquals(cloudId, updated.cloud?.deviceId)
        assertEquals(device.ble, updated.ble, "BLE transport must be preserved")
    }

    // endregion

    // region Test Setup

    private data class TestSetup(
        val watcher: CloudFetcherWatcher,
        val storage: FakePersistedStorage,
        val cloudBarsApi: FakeCloudBarsApi
    )

    private fun TestScope.createSetup(
        devices: List<BUSYBar> = emptyList(),
        isNetworkAvailable: Boolean = true,
        networkFlow: MutableStateFlow<Boolean>? = null,
        principal: BUSYLibUserPrincipal = BUSYLibUserPrincipal.Empty,
        principalFlow: MutableStateFlow<BUSYLibUserPrincipal>? = null,
        cloudBarsResult: Result<List<BusyCloudBar>> = Result.success(emptyList())
    ): TestSetup {
        val storage = FakePersistedStorage(MutableStateFlow(devices))
        val networkApi = FakeNetworkStateApi(networkFlow ?: MutableStateFlow(isNetworkAvailable))
        val principalApi = FakePrincipalApi(principalFlow ?: MutableStateFlow(principal))
        val cloudBarsApi = FakeCloudBarsApi(cloudBarsResult)
        val cloudFetcher = CloudFetcher(
            principalApi = principalApi,
            networkStateApi = networkApi,
            busyCloudBarsApi = cloudBarsApi,
            wsApi = FakeCloudWebSocketApi
        )

        val scope = CoroutineScope(
            SupervisorJob(backgroundScope.coroutineContext.job) +
                StandardTestDispatcher(testScheduler)
        )

        val watcher = CloudFetcherWatcher(
            scope = scope,
            persistedStorage = storage,
            cloudFetcher = cloudFetcher
        )

        return TestSetup(watcher, storage, cloudBarsApi)
    }

    private fun fakeToken(
        userId: Uuid = Uuid.random(),
        token: String = "token-$userId"
    ): BUSYLibUserPrincipal.Token = object : BUSYLibUserPrincipal.Token {
        override val userId: Uuid = userId
        override suspend fun getToken(failedToken: String?): String = token
    }

    private fun cloudBar(
        id: Uuid,
        label: String?,
        hardwareId: String = "hw-$id"
    ): BusyCloudBar = BusyCloudBar(id = id, hardwareId = hardwareId, label = label)

    private fun busyBar(id: String, vararg connectionWays: BUSYBar.ConnectionWay): BUSYBar {
        require(connectionWays.isNotEmpty()) { "At least one connection way is required" }
        val first = connectionWays.first()
        var result = when (first) {
            is BUSYBar.ConnectionWay.BLE -> BUSYBar(humanReadableName = DEFAULT_TEST_BAR_NAME, uniqueId = id, ble = first)
            is BUSYBar.ConnectionWay.Cloud -> BUSYBar(humanReadableName = DEFAULT_TEST_BAR_NAME, uniqueId = id, cloud = first)
            is BUSYBar.ConnectionWay.Lan -> BUSYBar(humanReadableName = DEFAULT_TEST_BAR_NAME, uniqueId = id, lan = first)
            is BUSYBar.ConnectionWay.Mock -> BUSYBar(humanReadableName = DEFAULT_TEST_BAR_NAME, uniqueId = id, mock = first)
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
        var transactionCount: Int = 0
            private set

        override fun getCurrentDeviceFlow() = currentDeviceFlow.asFlow().wrap()
        override fun getAllDevicesFlow() = devices.asFlow().wrap()

        override suspend fun addHook(vararg hook: TransactionHook) = Unit

        override suspend fun <T> transactionInternal(
            block: suspend InternalStorageTransactionScope.() -> T
        ): T {
            transactionCount++
            val scope = object : InternalStorageTransactionScope {
                override fun getCurrentDevice() = currentDeviceFlow.value
                override fun getAllDevices() = devices.value.toList()

                override fun setCurrentDevice(device: BUSYBar) {
                    currentDeviceFlow.value = device
                }

                override fun setCurrentDeviceNullable(device: BUSYBar?) {
                    currentDeviceFlow.value = device
                }

                override fun addOrReplace(device: BUSYBar) {
                    devices.update { list ->
                        list.filter { it.uniqueId != device.uniqueId } + device
                    }
                }

                override fun removeDevice(id: String) {
                    devices.update { list -> list.filter { it.uniqueId != id } }
                    if (currentDeviceFlow.value?.uniqueId == id) {
                        currentDeviceFlow.value = null
                    }
                }
            }
            return scope.block()
        }

        override suspend fun <T> transaction(
            block: suspend PersistedStorageTransactionScope.() -> T
        ): T = transactionInternal { block() }
    }

    private class FakeNetworkStateApi(
        flow: MutableStateFlow<Boolean>
    ) : BUSYLibNetworkStateApi {
        override val isNetworkAvailableFlow = flow.wrap()
    }

    private class FakePrincipalApi(
        private val flow: MutableStateFlow<BUSYLibUserPrincipal>
    ) : BUSYLibPrincipalApi {
        override fun getPrincipalFlow() = flow.wrap()
    }

    private class FakeCloudBarsApi(
        private val barsResult: Result<List<BusyCloudBar>>
    ) : BusyCloudBarsApi {
        var callCount: Int = 0
            private set

        override suspend fun getBarsList(
            principal: BUSYLibUserPrincipal.Token
        ): Result<List<BusyCloudBar>> {
            callCount++
            return barsResult
        }

        override suspend fun unlinkBusyBar(
            principal: BUSYLibUserPrincipal.Token,
            uuid: Uuid
        ): Result<Unit> = error("Not used in test")

        override suspend fun linkBusyBar(
            principal: BUSYLibUserPrincipal.Token,
            pin: String
        ): Result<Unit> = error("Not used in test")
    }

    private object FakeCloudWebSocketApi : CloudWebSocketApi {
        override fun getWSFlow(): Flow<BSBWebSocket?> = emptyFlow()
    }

    // endregion
}
