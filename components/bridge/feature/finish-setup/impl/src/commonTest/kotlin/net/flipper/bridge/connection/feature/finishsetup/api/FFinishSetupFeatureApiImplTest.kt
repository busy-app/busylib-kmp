@file:Suppress("MaximumLineLength", "LargeClass", "LongParameterList", "MaxLineLength")

package net.flipper.bridge.connection.feature.finishsetup.api

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.feature.ble.api.FBleFeatureApi
import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.finishsetup.krate.SetupFinishedBeforeKrate
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTask
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTaskStatus
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTaskType
import net.flipper.bridge.connection.feature.finishsetup.model.FFinishSetupState
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class FFinishSetupFeatureApiImplTest {

    private class FakeBleFeatureApi(
        initialStatus: FBleStatus? = null
    ) : FBleFeatureApi {
        private val _bleStatus = MutableStateFlow(initialStatus)

        @Suppress("UNCHECKED_CAST")
        override fun getBleStatus(): WrappedFlow<FBleStatus> =
            (_bleStatus as MutableStateFlow<FBleStatus>).asFlow().wrap()
    }

    private class FakeLinkedInfoOnDemandFeatureApi(
        initialStatus: LinkedAccountInfo
    ) : FLinkedInfoOnDemandFeatureApi {
        private val _status = MutableStateFlow(initialStatus)
        override val status: WrappedFlow<LinkedAccountInfo> = _status.asFlow().wrap()

        override suspend fun deleteAccount(): CResult<SuccessResponse> =
            CResult.Success(SuccessResponse("ok"))
    }

    private class FakeWiFiFeatureApi(
        initialStatus: StatusResponse
    ) : FWiFiFeatureApi {
        private val _wifiStatus = MutableStateFlow(initialStatus)

        override fun getWifiStatusFlow(): WrappedFlow<StatusResponse> = _wifiStatus.asFlow().wrap()

        override fun getWifiStateFlow(): WrappedFlow<ImmutableList<WiFiNetwork>> =
            MutableStateFlow(persistentListOf<WiFiNetwork>()).asFlow().wrap()

        override suspend fun connect(
            ssid: String,
            password: String,
            security: WiFiSecurity.Supported
        ): CResult<Unit> = CResult.Success(Unit)

        override suspend fun disconnect(): CResult<Unit> = CResult.Success(Unit)
    }

    private class FakeFirmwareUpdateFeatureApi(
        initialUpdateStatus: UpdateStatus
    ) : FFirmwareUpdateFeatureApi {
        private val _updateStatus = MutableStateFlow(initialUpdateStatus)
        override val updateStatusFlow: WrappedSharedFlow<UpdateStatus> = (_updateStatus.asStateFlow() as SharedFlow<UpdateStatus>).wrap()

        override suspend fun setAutoUpdate(isEnabled: Boolean): CResult<Unit> =
            CResult.Success(Unit)

        override suspend fun getAutoUpdate(): CResult<Boolean> = CResult.Success(true)

        override val updateVersionFlow: WrappedFlow<BsbUpdateVersion> =
            MutableStateFlow<BsbUpdateVersion>(BsbUpdateVersion.Default("1.0.0")).asFlow().wrap()

        override val updateVersionChangelog: WrappedFlow<String> =
            MutableStateFlow("").asFlow().wrap()
    }

    private class FakeSetupFinishedBeforeKrate(
        initialValue: Boolean = false
    ) : SetupFinishedBeforeKrate {
        private val _state = MutableStateFlow(initialValue)
        override val cachedStateFlow: StateFlow<Boolean> = _state.asStateFlow()
        var savedValue: Boolean? = null

        override suspend fun save(value: Boolean) {
            savedValue = value
            _state.value = value
        }

        override suspend fun reset() {
            _state.value = false
            savedValue = false
        }

        override suspend fun getValue(): Boolean = _state.value
    }

    private fun defaultUpdateStatus(
        checkResult: UpdateStatus.Check.CheckResult = UpdateStatus.Check.CheckResult.NOT_AVAILABLE
    ) = UpdateStatus(
        install = UpdateStatus.Install(
            isAllowed = false,
            event = UpdateStatus.Install.Event.NONE,
            action = UpdateStatus.Install.Action.NONE,
            status = UpdateStatus.Install.Status.OK,
            detail = "",
            download = UpdateStatus.Install.Download(
                speedBytesPerSec = 0,
                receivedBytes = 0,
                totalBytes = 0
            )
        ),
        check = UpdateStatus.Check(
            availableVersion = "",
            event = UpdateStatus.Check.CheckEvent.NONE,
            status = checkResult
        )
    )

    private fun wifiStatus(state: StatusResponse.State) = StatusResponse(state = state)

    private val testUuid = Uuid.parse("00000000-0000-0000-0000-000000000001")

    private fun createImpl(
        scope: CoroutineScope,
        fBleFeatureApi: FBleFeatureApi? = null,
        linkedAccountInfo: LinkedAccountInfo = LinkedAccountInfo.NotLinked,
        wifiStatus: StatusResponse = wifiStatus(StatusResponse.State.CONNECTED),
        updateStatus: UpdateStatus = defaultUpdateStatus(),
        isSetupFinishedBefore: Boolean = false
    ): FFinishSetupFeatureApiImpl {
        return FFinishSetupFeatureApiImpl(
            scope = scope,
            fBleFeatureApi = fBleFeatureApi,
            fLinkedInfoOnDemandFeatureApi = FakeLinkedInfoOnDemandFeatureApi(linkedAccountInfo),
            fWiFiFeatureApi = FakeWiFiFeatureApi(wifiStatus),
            fFirmwareUpdateFeatureApi = FakeFirmwareUpdateFeatureApi(updateStatus),
            setupFinishedBeforeKrate = FakeSetupFinishedBeforeKrate(isSetupFinishedBefore)
        )
    }

    private fun task(type: DeviceSetupTaskType, status: DeviceSetupTaskStatus) =
        DeviceSetupTask(type = type, status = status)

    @Test
    fun GIVEN_setup_finished_before_WHEN_any_states_THEN_finished_before() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(),
            isSetupFinishedBefore = true
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.FinishedBefore, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_all_null_WHEN_ble_feature_present_THEN_loading() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(null),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.Loading, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_linked_null_WHEN_ble_and_wifi_ready_THEN_no_emission() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val linkedFlow = MutableSharedFlow<LinkedAccountInfo>()
        val impl = FFinishSetupFeatureApiImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            fLinkedInfoOnDemandFeatureApi = object : FLinkedInfoOnDemandFeatureApi {
                override val status: WrappedFlow<LinkedAccountInfo> = linkedFlow.asFlow().wrap()
                override suspend fun deleteAccount(): CResult<SuccessResponse> =
                    CResult.Success(SuccessResponse("ok"))
            },
            fWiFiFeatureApi = FakeWiFiFeatureApi(wifiStatus(StatusResponse.State.CONNECTED)),
            fFirmwareUpdateFeatureApi = FakeFirmwareUpdateFeatureApi(defaultUpdateStatus()),
            setupFinishedBeforeKrate = FakeSetupFinishedBeforeKrate(false)
        )

        advanceUntilIdle()
        val result = withTimeoutOrNull(1.seconds) {
            impl.taskListResourceFlow.first()
        }
        assertNull(result)
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_null_WHEN_ble_and_linked_ready_THEN_no_emission() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val wifiFlow = MutableSharedFlow<StatusResponse>()
        val impl = FFinishSetupFeatureApiImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            fLinkedInfoOnDemandFeatureApi = FakeLinkedInfoOnDemandFeatureApi(LinkedAccountInfo.NotLinked),
            fWiFiFeatureApi = object : FWiFiFeatureApi {
                override fun getWifiStatusFlow(): WrappedFlow<StatusResponse> = wifiFlow.asFlow().wrap()
                override fun getWifiStateFlow(): WrappedFlow<ImmutableList<WiFiNetwork>> =
                    MutableStateFlow(persistentListOf<WiFiNetwork>()).asFlow().wrap()
                override suspend fun connect(
                    ssid: String,
                    password: String,
                    security: WiFiSecurity.Supported
                ): CResult<Unit> = CResult.Success(Unit)
                override suspend fun disconnect(): CResult<Unit> = CResult.Success(Unit)
            },
            fFirmwareUpdateFeatureApi = FakeFirmwareUpdateFeatureApi(defaultUpdateStatus()),
            setupFinishedBeforeKrate = FakeSetupFinishedBeforeKrate(false)
        )

        advanceUntilIdle()
        val result = withTimeoutOrNull(1.seconds) {
            impl.taskListResourceFlow.first()
        }
        assertNull(result)
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_feature_present_but_status_null_WHEN_others_ready_THEN_loading() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(null),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.Loading, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_initialization_WHEN_others_ready_THEN_loading() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Initialization),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.Loading, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_connecting_WHEN_others_ready_THEN_loading() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTING),
            updateStatus = defaultUpdateStatus(),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.Loading, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_reconnecting_WHEN_others_ready_THEN_loading() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.RECONNECTING),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.Loading, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_all_completed_with_ble_THEN_finished_before() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.FinishedBefore, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_no_ble_feature_all_completed_THEN_finished_before() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = null,
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.FinishedBefore, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_enabled_not_connected_WHEN_others_completed_THEN_loaded_with_ble_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Enabled),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.COMPLETED),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_disabled_WHEN_others_completed_THEN_loaded_with_ble_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Disabled),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, result.tasks[0].status)
        assertEquals(DeviceSetupTaskType.PAIR_BLE, result.tasks[0].type)
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_internal_error_WHEN_others_completed_THEN_loaded_with_ble_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.InternalError),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, result.tasks[0].status)
        assertEquals(DeviceSetupTaskType.PAIR_BLE, result.tasks[0].type)
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_connectable_WHEN_others_completed_THEN_loaded_with_ble_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connectable),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, result.tasks[0].status)
        assertEquals(DeviceSetupTaskType.PAIR_BLE, result.tasks[0].type)
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_reset_WHEN_others_completed_THEN_loaded_with_ble_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Reset),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, result.tasks[0].status)
        assertEquals(DeviceSetupTaskType.PAIR_BLE, result.tasks[0].type)
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_disconnected_ble_connected_same_user_THEN_loaded() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_AVAILABLE),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_disconnecting_THEN_loaded_with_wifi_not_available() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTING),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.COMPLETED),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_unknown_THEN_loaded_with_wifi_not_available() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.UNKNOWN),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.COMPLETED),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_not_linked_wifi_connected_THEN_link_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val linkTask = result.tasks.first { it.type == DeviceSetupTaskType.LINK_ACCOUNT }
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, linkTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_not_linked_wifi_disconnected_THEN_link_not_available() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_AVAILABLE),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_different_user_wifi_connected_THEN_link_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.DifferentUser(testUuid, "x@y.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val linkTask = result.tasks.first { it.type == DeviceSetupTaskType.LINK_ACCOUNT }
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, linkTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_missing_busy_cloud_wifi_connected_THEN_link_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.MissingBusyCloud(testUuid, "m@c.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val linkTask = result.tasks.first { it.type == DeviceSetupTaskType.LINK_ACCOUNT }
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, linkTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_linked_error_wifi_connected_THEN_link_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Error,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val linkTask = result.tasks.first { it.type == DeviceSetupTaskType.LINK_ACCOUNT }
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, linkTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_linked_disconnected_wifi_connected_THEN_link_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Disconnected,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val linkTask = result.tasks.first { it.type == DeviceSetupTaskType.LINK_ACCOUNT }
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, linkTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_different_user_wifi_not_available_THEN_link_not_available() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.DifferentUser(testUuid, "x@y.com"),
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTING),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.COMPLETED),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_update_available_wifi_connected_THEN_update_not_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val fwTask = result.tasks.first { it.type == DeviceSetupTaskType.UPDATE_FIRMWARE }
        assertEquals(DeviceSetupTaskStatus.NOT_COMPLETED, fwTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_update_not_available_wifi_connected_all_done_THEN_finished_before() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.FinishedBefore, result)
        scope.cancel()
    }

    @Test
    fun GIVEN_update_check_failure_wifi_connected_THEN_update_not_available() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.FAILURE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val fwTask = result.tasks.first { it.type == DeviceSetupTaskType.UPDATE_FIRMWARE }
        assertEquals(DeviceSetupTaskStatus.NOT_AVAILABLE, fwTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_update_check_none_wifi_connected_THEN_update_loading() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NONE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val fwTask = result.tasks.first { it.type == DeviceSetupTaskType.UPDATE_FIRMWARE }
        assertEquals(DeviceSetupTaskStatus.LOADING, fwTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_update_available_wifi_disconnected_THEN_update_not_available() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val fwTask = result.tasks.first { it.type == DeviceSetupTaskType.UPDATE_FIRMWARE }
        assertEquals(DeviceSetupTaskStatus.NOT_AVAILABLE, fwTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_not_available_THEN_update_firmware_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTING),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        val fwTask = result.tasks.first { it.type == DeviceSetupTaskType.UPDATE_FIRMWARE }
        assertEquals(DeviceSetupTaskStatus.COMPLETED, fwTask.status)
        scope.cancel()
    }

    @Test
    fun GIVEN_nothing_done_THEN_loaded_with_ble_and_wifi_not_completed_rest_not_available() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Enabled),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_AVAILABLE),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_connected_wifi_disconnected_not_linked_THEN_wifi_step_next() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_AVAILABLE),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_ble_wifi_completed_not_linked_update_available_THEN_two_tasks_remaining() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_COMPLETED),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_no_ble_wifi_disconnected_THEN_loaded_3_tasks() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = null,
            linkedAccountInfo = LinkedAccountInfo.NotLinked,
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(3, result.tasks.size)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_AVAILABLE),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_no_ble_almost_done_update_available_THEN_only_firmware_remaining() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = null,
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(3, result.tasks.size)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_COMPLETED),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_same_user_wifi_disconnected_THEN_link_still_completed() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.DISCONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_COMPLETED),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.NOT_AVAILABLE),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_wifi_not_available_THEN_firmware_completed_skip_logic() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = FakeBleFeatureApi(FBleStatus.Connected("AA:BB:CC")),
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.UNKNOWN),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertIs<FFinishSetupState.Loaded>(result)
        assertEquals(
            listOf(
                task(DeviceSetupTaskType.PAIR_BLE, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.CONNECT_WIFI, DeviceSetupTaskStatus.NOT_AVAILABLE),
                task(DeviceSetupTaskType.LINK_ACCOUNT, DeviceSetupTaskStatus.COMPLETED),
                task(DeviceSetupTaskType.UPDATE_FIRMWARE, DeviceSetupTaskStatus.COMPLETED),
            ),
            result.tasks
        )
        scope.cancel()
    }

    @Test
    fun GIVEN_no_ble_feature_THEN_ble_null_does_not_block_loading() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        val impl = createImpl(
            scope = scope,
            fBleFeatureApi = null,
            linkedAccountInfo = LinkedAccountInfo.Linked.SameUser(testUuid, "a@b.com"),
            wifiStatus = wifiStatus(StatusResponse.State.CONNECTED),
            updateStatus = defaultUpdateStatus(UpdateStatus.Check.CheckResult.NOT_AVAILABLE),
            isSetupFinishedBefore = false
        )

        val result = impl.taskListResourceFlow.first()
        assertEquals(FFinishSetupState.FinishedBefore, result)
        scope.cancel()
    }
}
