package net.flipper.tools.oncall.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.model.OnCallAnimation
import net.flipper.bridge.connection.feature.oncall.impl.OnCallDisplayLoop
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.tools.oncall.impl.api.CloudAssetsApiLauncher
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class OnCallSingletonApiImplTest {

    @Test
    fun GIVEN_local_device_WHEN_start_THEN_feature_started_with_animation() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(LOCAL_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.MEETING)
        runCurrent()

        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.MEETING)), env.feature.calls)
    }

    @Test
    fun GIVEN_local_on_call_WHEN_animation_changed_THEN_feature_started_with_new_animation() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(LOCAL_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.start(OnCallAnimation.CODING)
        runCurrent()

        assertEquals(
            listOf<Call>(Call.Start(OnCallAnimation.ON_CALL), Call.Start(OnCallAnimation.CODING)),
            env.feature.calls
        )
    }

    @Test
    fun GIVEN_local_on_call_WHEN_start_with_same_animation_THEN_feature_not_restarted() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(LOCAL_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()

        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.ON_CALL)), env.feature.calls)
    }

    @Test
    fun GIVEN_local_on_call_WHEN_stop_THEN_feature_stopped() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(LOCAL_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.stop()
        runCurrent()

        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.ON_CALL), Call.Stop), env.feature.calls)
    }

    @Test
    fun GIVEN_stopped_WHEN_start_again_THEN_feature_started_with_latest_animation() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(LOCAL_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.stop()
        runCurrent()
        env.api.start(OnCallAnimation.LUNCH)
        runCurrent()

        assertEquals(
            listOf<Call>(Call.Start(OnCallAnimation.ON_CALL), Call.Stop, Call.Start(OnCallAnimation.LUNCH)),
            env.feature.calls
        )
    }

    @Test
    fun GIVEN_local_device_with_on_call_disabled_WHEN_start_THEN_feature_not_started() = runTest {
        val env = Env(backgroundScope)
        val disabledBar = LOCAL_BAR.disabled()
        env.storage.devices.value = listOf(disabledBar)
        env.connect(disabledBar)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()

        assertTrue(env.feature.calls.none { call -> call is Call.Start })
    }

    @Test
    fun GIVEN_local_on_call_WHEN_device_disables_on_call_THEN_feature_stopped() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(LOCAL_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.storage.devices.value = listOf(LOCAL_BAR.disabled())
        runCurrent()

        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.ON_CALL), Call.Stop), env.feature.calls)
    }

    @Test
    fun GIVEN_cloud_device_WHEN_start_THEN_animation_drawn_through_cloud() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(CLOUD_BAR)

        env.api.start(OnCallAnimation.FLOW)
        runCurrent()

        assertEquals(listOf(CLOUD_ID), env.launcher.openedDeviceIds)
        assertEquals(listOf("shared/flow_72x16.anim"), env.launcher.assetsApi(CLOUD_ID).drawnPaths)

        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(
            listOf("shared/flow_72x16.anim", "shared/flow_72x16.anim"),
            env.launcher.assetsApi(CLOUD_ID).drawnPaths
        )
    }

    @Test
    fun GIVEN_cloud_on_call_WHEN_animation_changed_THEN_draw_switches_in_same_session() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(CLOUD_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.start(OnCallAnimation.ON_AIR)
        runCurrent()

        val assetsApi = env.launcher.assetsApi(CLOUD_ID)
        assertEquals(listOf("shared/on_call_72x16.anim", "shared/on_air_72x16.anim"), assetsApi.drawnPaths)
        assertEquals(emptyList(), assetsApi.removedAppIds)
        assertEquals(listOf(CLOUD_ID), env.launcher.openedDeviceIds)

        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(
            listOf("shared/on_call_72x16.anim", "shared/on_air_72x16.anim", "shared/on_air_72x16.anim"),
            assetsApi.drawnPaths
        )
    }

    @Test
    fun GIVEN_cloud_on_call_WHEN_stop_THEN_draw_removed_and_session_closed() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(CLOUD_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.stop()
        runCurrent()

        assertEquals(listOf("busy_lib_on_call"), env.launcher.assetsApi(CLOUD_ID).removedAppIds)
        assertEquals(0, env.launcher.activeSessions)

        advanceTimeBy(30.seconds)
        assertEquals(1, env.launcher.assetsApi(CLOUD_ID).drawnPaths.size)
    }

    @Test
    fun GIVEN_cloud_device_with_on_call_disabled_WHEN_start_THEN_no_cloud_session() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(CLOUD_BAR.disabled())

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()

        assertEquals(emptyList(), env.launcher.openedDeviceIds)
    }

    @Test
    fun GIVEN_cloud_on_call_WHEN_device_removed_from_storage_THEN_draw_removed() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(CLOUD_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.storage.devices.value = emptyList()
        runCurrent()

        assertEquals(listOf("busy_lib_on_call"), env.launcher.assetsApi(CLOUD_ID).removedAppIds)
        assertEquals(0, env.launcher.activeSessions)
    }

    @Test
    fun GIVEN_device_connected_locally_WHEN_start_THEN_only_local_route_draws() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(HYBRID_BAR)
        env.connect(HYBRID_BAR)
        runCurrent()

        env.api.start(OnCallAnimation.DND)
        runCurrent()

        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.DND)), env.feature.calls)
        assertEquals(emptyList(), env.launcher.assetsApi(HYBRID_CLOUD_ID).drawnPaths)
    }

    @Test
    fun GIVEN_local_on_call_WHEN_local_device_disconnects_THEN_cloud_draws_current_animation() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(HYBRID_BAR)
        env.connect(HYBRID_BAR)
        runCurrent()

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.start(OnCallAnimation.BOOKED)
        runCurrent()
        env.disconnect()
        runCurrent()

        assertEquals(listOf("shared/booked_72x16.anim"), env.launcher.assetsApi(HYBRID_CLOUD_ID).drawnPaths)
    }

    @Test
    fun GIVEN_local_feature_retrieving_WHEN_start_THEN_cloud_waits_and_local_takes_over() = runTest {
        val env = Env(backgroundScope)
        env.featureProvider.status.value = FFeatureStatus.Retrieving
        env.storage.devices.value = listOf(HYBRID_BAR)
        env.connect(HYBRID_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        advanceTimeBy(30.seconds)

        assertEquals(emptyList(), env.launcher.assetsApi(HYBRID_CLOUD_ID).drawnPaths)
        assertEquals(emptyList(), env.feature.calls)

        env.featureProvider.status.value = FFeatureStatus.Supported(env.feature)
        runCurrent()

        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.ON_CALL)), env.feature.calls)
        assertEquals(emptyList(), env.launcher.assetsApi(HYBRID_CLOUD_ID).drawnPaths)
    }

    @Test
    fun GIVEN_local_feature_unsupported_WHEN_start_THEN_cloud_draws() = runTest {
        val env = Env(backgroundScope)
        env.featureProvider.status.value = FFeatureStatus.Unsupported
        env.storage.devices.value = listOf(HYBRID_BAR)
        env.connect(HYBRID_BAR)

        env.api.start(OnCallAnimation.MEETING)
        runCurrent()

        assertEquals(listOf("shared/meeting_72x16.anim"), env.launcher.assetsApi(HYBRID_CLOUD_ID).drawnPaths)
        assertEquals(emptyList(), env.feature.calls)
    }

    @Test
    fun GIVEN_local_feature_not_found_WHEN_start_THEN_cloud_draws() = runTest {
        val env = Env(backgroundScope)
        env.featureProvider.status.value = FFeatureStatus.NotFound
        env.storage.devices.value = listOf(HYBRID_BAR)
        env.connect(HYBRID_BAR)

        env.api.start(OnCallAnimation.MEETING)
        runCurrent()

        assertEquals(listOf("shared/meeting_72x16.anim"), env.launcher.assetsApi(HYBRID_CLOUD_ID).drawnPaths)
    }

    @Test
    fun GIVEN_other_device_retrieving_WHEN_start_THEN_cloud_device_draws_without_waiting() = runTest {
        val env = Env(backgroundScope)
        env.featureProvider.status.value = FFeatureStatus.Retrieving
        env.storage.devices.value = listOf(LOCAL_BAR, CLOUD_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.FLOW)
        runCurrent()

        assertEquals(listOf("shared/flow_72x16.anim"), env.launcher.assetsApi(CLOUD_ID).drawnPaths)
    }

    @Test
    fun GIVEN_cloud_on_call_WHEN_device_reconnects_THEN_cloud_draws_until_local_ready() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(HYBRID_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.featureProvider.status.value = FFeatureStatus.Retrieving
        env.connect(HYBRID_BAR)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()

        val assetsApi = env.launcher.assetsApi(HYBRID_CLOUD_ID)
        assertEquals(2, assetsApi.drawnPaths.size)
        assertEquals(emptyList(), assetsApi.removedAppIds)

        env.featureProvider.status.value = FFeatureStatus.Supported(env.feature)
        runCurrent()
        advanceTimeBy(30.seconds)

        assertEquals(2, assetsApi.drawnPaths.size)
        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.ON_CALL)), env.feature.calls)
    }

    @Test
    fun GIVEN_cloud_on_call_WHEN_device_connects_locally_THEN_cloud_stops_drawing_without_remove() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(HYBRID_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.connect(HYBRID_BAR)
        runCurrent()

        val assetsApi = env.launcher.assetsApi(HYBRID_CLOUD_ID)
        assertEquals(listOf("shared/on_call_72x16.anim"), assetsApi.drawnPaths)
        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.ON_CALL)), env.feature.calls)

        advanceTimeBy(30.seconds)
        assertEquals(1, assetsApi.drawnPaths.size)

        env.api.stop()
        runCurrent()
        assertEquals(emptyList(), assetsApi.removedAppIds)
    }

    @Test
    fun GIVEN_cloud_on_call_WHEN_stop_and_start_immediately_THEN_sessions_never_overlap() = runTest {
        val env = Env(backgroundScope)
        env.storage.devices.value = listOf(CLOUD_BAR)

        env.api.start(OnCallAnimation.ON_CALL)
        runCurrent()
        env.api.stop()
        env.api.start(OnCallAnimation.KEEP_OUT)
        runCurrent()
        env.api.stop()
        runCurrent()
        env.api.start(OnCallAnimation.LOW_SOCIAL_BATTERY)
        runCurrent()

        assertEquals(1, env.launcher.maxActiveSessions)
        assertEquals(1, env.launcher.activeSessions)
        assertEquals(
            "shared/low_social_battery_72x16.anim",
            env.launcher.assetsApi(CLOUD_ID).drawnPaths.last()
        )
    }

    @Test
    fun GIVEN_cloud_launcher_fails_WHEN_start_THEN_other_routes_still_draw() = runTest {
        val env = Env(backgroundScope)
        env.launcher.failingDeviceIds += CLOUD_ID
        env.storage.devices.value = listOf(CLOUD_BAR, LOCAL_BAR, SECOND_CLOUD_BAR)
        env.connect(LOCAL_BAR)

        env.api.start(OnCallAnimation.CHILL_TIME)
        runCurrent()

        assertEquals(listOf<Call>(Call.Start(OnCallAnimation.CHILL_TIME)), env.feature.calls)
        assertEquals(
            listOf("shared/chill_time_72x16.anim"),
            env.launcher.assetsApi(SECOND_CLOUD_ID).drawnPaths
        )
    }

    private fun BUSYBar.disabled(): BUSYBar = copy(humanReadableName = humanReadableName, onCallEnabled = false)

    private class Env(private val scope: CoroutineScope) {
        val feature = FakeOnCallFeatureApi()
        val orchestrator = FakeDeviceOrchestrator()
        val storage = FakeDevicePersistedStorage()
        val launcher = FakeCloudAssetsApiLauncher()
        val featureProvider = FakeFeatureProvider(feature)
        val api = OnCallSingletonApiImpl(
            scope = scope,
            featureProvider = featureProvider,
            devicePersistedStorage = storage,
            orchestrator = orchestrator,
            cloudAssetsApiLauncher = launcher,
            displayLoopFactory = OnCallDisplayLoop.Factory { assetsApi -> OnCallDisplayLoop(assetsApi) }
        )

        fun connect(bar: BUSYBar) {
            orchestrator.state.value = FDeviceConnectStatus.Connected(
                scope = scope,
                device = bar,
                deviceApi = FakeConnectedDeviceApi(),
                transportType = FDeviceTransportType.LAN
            )
        }

        fun disconnect() {
            orchestrator.state.value = FDeviceConnectStatus.Disconnected(
                device = null,
                reason = DisconnectStatus.REPORTED_BY_TRANSPORT
            )
        }
    }

    private companion object {
        val CLOUD_ID = Uuid.parse("00000000-0000-0000-0000-000000000001")
        val SECOND_CLOUD_ID = Uuid.parse("00000000-0000-0000-0000-000000000002")
        val HYBRID_CLOUD_ID = Uuid.parse("00000000-0000-0000-0000-000000000003")

        val LOCAL_BAR = BUSYBar(
            humanReadableName = "Local",
            uniqueId = "local",
            mock = BUSYBar.ConnectionWay.Mock
        )
        val CLOUD_BAR = BUSYBar(
            humanReadableName = "Cloud",
            uniqueId = "cloud",
            cloud = BUSYBar.ConnectionWay.Cloud(CLOUD_ID)
        )
        val SECOND_CLOUD_BAR = BUSYBar(
            humanReadableName = "Second cloud",
            uniqueId = "second_cloud",
            cloud = BUSYBar.ConnectionWay.Cloud(SECOND_CLOUD_ID)
        )
        val HYBRID_BAR = BUSYBar(
            humanReadableName = "Hybrid",
            uniqueId = "hybrid",
            cloud = BUSYBar.ConnectionWay.Cloud(HYBRID_CLOUD_ID)
        ).addTransport(lan = BUSYBar.ConnectionWay.Lan)
    }
}

private sealed interface Call {
    data class Start(val animation: OnCallAnimation) : Call
    data object Stop : Call
}

private class FakeOnCallFeatureApi : FOnCallFeatureApi {
    val calls = mutableListOf<Call>()

    override fun start(animation: OnCallAnimation) {
        calls += Call.Start(animation)
    }

    override fun stop() {
        calls += Call.Stop
    }
}

private class FakeFeatureProvider(feature: FDeviceFeatureApi) : FFeatureProvider {
    val status = MutableStateFlow<FFeatureStatus<FDeviceFeatureApi>>(FFeatureStatus.Supported(feature))

    override fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>> = error("Not used")

    @Suppress("UNCHECKED_CAST")
    override fun <T : FDeviceFeatureApi> getFiltered(
        status: FDeviceConnectStatus.Connected,
        clazz: KClass<T>
    ): Flow<FFeatureStatus<T>> = this.status as Flow<FFeatureStatus<T>>

    override suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T? = error("Not used")
}

private class FakeDeviceOrchestrator : FDeviceOrchestrator {
    val state = MutableStateFlow<FDeviceConnectStatus>(
        FDeviceConnectStatus.Disconnected(device = null, reason = DisconnectStatus.NOT_INITIALIZED)
    )

    override fun getState(): WrappedStateFlow<FDeviceConnectStatus> = state.wrap()
}

private class FakeDevicePersistedStorage : FDevicePersistedStorage {
    val devices = MutableStateFlow<List<BUSYBar>>(emptyList())

    override fun getCurrentDeviceFlow(): WrappedFlow<BUSYBar?> = error("Not used")

    override fun getAllDevicesFlow(): WrappedFlow<List<BUSYBar>> = WrappedFlow(devices)

    override suspend fun <T> transaction(block: suspend PersistedStorageTransactionScope.() -> T): T = error("Not used")
}

private class FakeConnectedDeviceApi : FConnectedDeviceApi {
    override val deviceName: String = "Fake"

    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> = error("Not used")

    override suspend fun disconnect() = Unit
}

private class FakeCloudAssetsApiLauncher : CloudAssetsApiLauncher {
    val openedDeviceIds = mutableListOf<Uuid>()
    val failingDeviceIds = mutableSetOf<Uuid>()
    var activeSessions = 0
        private set
    var maxActiveSessions = 0
        private set
    private val assetsApis = mutableMapOf<Uuid, FakeRpcAssetsApi>()

    fun assetsApi(deviceId: Uuid): FakeRpcAssetsApi = assetsApis.getOrPut(deviceId) { FakeRpcAssetsApi() }

    override suspend fun withAssetsApi(deviceId: Uuid, block: suspend (FRpcAssetsApi) -> Unit): Result<Unit> {
        openedDeviceIds += deviceId
        if (deviceId in failingDeviceIds) return Result.failure(IllegalStateException("Cloud unavailable"))
        activeSessions++
        maxActiveSessions = maxOf(maxActiveSessions, activeSessions)
        return try {
            block(assetsApi(deviceId))
            Result.success(Unit)
        } finally {
            activeSessions--
        }
    }
}

private class FakeRpcAssetsApi : FRpcAssetsApi {
    val drawRequests = mutableListOf<DrawRequest>()
    val removedAppIds = mutableListOf<String>()
    val drawnPaths: List<String?> get() = drawRequests.map { request -> request.elements.single().stockPath }

    override suspend fun uploadAsset(appId: String, file: String, content: ByteArray): Result<SuccessResponse> {
        return Result.success(SuccessResponse("ok"))
    }

    override suspend fun displayDraw(request: DrawRequest): Result<SuccessResponse> {
        drawRequests += request
        return Result.success(SuccessResponse("ok"))
    }

    override suspend fun removeDraw(appId: String): Result<SuccessResponse> {
        removedAppIds += appId
        return Result.success(SuccessResponse("ok"))
    }
}
