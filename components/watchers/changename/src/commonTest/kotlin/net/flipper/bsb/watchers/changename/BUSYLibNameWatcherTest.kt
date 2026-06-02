package net.flipper.bsb.watchers.changename

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.settings.api.FSettingsFeatureApi
import net.flipper.bridge.connection.feature.settings.model.BsbBrightness
import net.flipper.bridge.connection.feature.settings.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.settings.model.BsbVolume
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.data.Fraction
import net.flipper.core.busylib.ktx.common.asFlow
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BUSYLibNameWatcherTest {

    @Test
    fun GIVEN_connected_WHEN_device_disconnects_THEN_late_name_from_that_device_is_ignored() =
        runTest {
            // Reproduces the wrong-device leak: once the device is gone, its settings flow must
            // stop being consumed. A late name emission from the detached feature must be ignored.
            val device = BUSYBar(
                humanReadableName = "Original",
                uniqueId = "device-1",
                ble = BUSYBar.ConnectionWay.BLE("AA:BB:CC")
            )
            val storage = FakePersistedStorage(MutableStateFlow(listOf(device)))
            val nameFlow = MutableStateFlow("Original")
            val featureFlow = MutableStateFlow<FFeatureStatus<FSettingsFeatureApi>>(
                FFeatureStatus.Supported(FakeSettingsFeatureApi(nameFlow))
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
            val watcher = BUSYLibNameWatcher(
                scope = scope,
                featureProvider = FakeFeatureProvider(featureFlow),
                orchestrator = FakeOrchestrator(orchestratorState),
                persistedStorage = storage
            )

            watcher.onLaunch()
            advanceUntilIdle()

            // The live device renames itself; the watcher must persist it.
            nameFlow.value = "Live name"
            advanceUntilIdle()
            assertEquals("Live name", storage.find("device-1")?.humanReadableName)

            // Device drops and nothing reconnects.
            orchestratorState.value = FDeviceConnectStatus.Disconnected(
                device = device,
                reason = DisconnectStatus.REPORTED_BY_TRANSPORT
            )
            advanceUntilIdle()

            // The detached feature emits a new name after the device is gone.
            nameFlow.value = "Name after disconnect"
            advanceUntilIdle()

            assertEquals(
                "Live name",
                storage.find("device-1")?.humanReadableName,
                "A name emitted after the device disconnected must not be applied"
            )
        }

    private class FakeOrchestrator(
        private val stateFlow: MutableStateFlow<FDeviceConnectStatus>
    ) : FDeviceOrchestrator {
        override fun getState(): WrappedStateFlow<FDeviceConnectStatus> = stateFlow.wrap()
    }

    private class FakeFeatureProvider(
        private val settingsFlow: Flow<FFeatureStatus<FSettingsFeatureApi>>
    ) : FFeatureProvider {
        @Suppress("UNCHECKED_CAST")
        override fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>> =
            settingsFlow as Flow<FFeatureStatus<T>>

        override fun <T : FDeviceFeatureApi> getFiltered(
            status: FDeviceConnectStatus.Connected,
            clazz: KClass<T>
        ): Flow<FFeatureStatus<T>> = get(clazz)

        override suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T? = null
    }

    private class FakeSettingsFeatureApi(
        private val deviceName: StateFlow<String>
    ) : FSettingsFeatureApi {
        override fun getDeviceName(): WrappedStateFlow<String> = deviceName.wrap()

        override fun getVolumeFlow(): WrappedFlow<BsbVolume> = error("Not used in test")
        override fun getBrightnessInfoFlow(): WrappedFlow<BsbBrightnessInfo> = error("Not used in test")
        override suspend fun setBrightness(value: BsbBrightness): CResult<Unit> = error("Not used in test")
        override suspend fun setVolume(volume: Fraction): CResult<Unit> = error("Not used in test")
        override suspend fun setDeviceName(name: String): CResult<Unit> = error("Not used in test")
    }

    private class FakeConnectedDeviceApi : FConnectedDeviceApi {
        override val deviceName = "Test Device"
        override suspend fun tryUpdateConnectionConfig(
            config: FDeviceConnectionConfig<*>
        ): Result<Unit> = Result.failure(NotImplementedError())

        override suspend fun disconnect() = Unit
    }

    private class FakePersistedStorage(
        val devices: MutableStateFlow<List<BUSYBar>>
    ) : FDevicePersistedStorage {
        private val currentDevice = MutableStateFlow<BUSYBar?>(null)

        fun find(id: String): BUSYBar? = devices.value.firstOrNull { it.uniqueId == id }

        override fun getCurrentDeviceFlow(): WrappedFlow<BUSYBar?> = currentDevice.asFlow().wrap()
        override fun getAllDevicesFlow(): WrappedFlow<List<BUSYBar>> = devices.asFlow().wrap()

        override suspend fun <T> transaction(
            block: suspend PersistedStorageTransactionScope.() -> T
        ): T {
            val scope = object : PersistedStorageTransactionScope {
                override fun getCurrentDevice(): BUSYBar? = currentDevice.value
                override fun getAllDevices(): List<BUSYBar> = devices.value.toList()
                override fun setCurrentDevice(device: BUSYBar) {
                    currentDevice.value = device
                }

                override fun addOrReplace(device: BUSYBar) {
                    devices.update { list ->
                        list.filter { it.uniqueId != device.uniqueId } + device
                    }
                }
            }
            return scope.block()
        }
    }
}
