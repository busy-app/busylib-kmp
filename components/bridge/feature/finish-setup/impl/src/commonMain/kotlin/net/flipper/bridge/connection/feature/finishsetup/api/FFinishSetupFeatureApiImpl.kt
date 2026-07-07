package net.flipper.bridge.connection.feature.finishsetup.api

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.ble.api.FBleFeatureApi
import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.finishsetup.krate.SetupFinishedBeforeKrate
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTask
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTaskStatus
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTaskType
import net.flipper.bridge.connection.feature.finishsetup.model.FFinishSetupState
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatus
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.platform.BusyLibPlatform
import net.flipper.core.busylib.ktx.common.platform.currentPlatform
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds

class FFinishSetupFeatureApiImpl(
    private val scope: CoroutineScope,
    private val fBleFeatureApi: FBleFeatureApi?,
    private val fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
    private val fWiFiFeatureApi: FWiFiFeatureApi,
    private val fFirmwareUpdateFeatureApi: FFirmwareUpdateFeatureApi,
    private val setupFinishedBeforeKrate: SetupFinishedBeforeKrate
) : FFinishSetupFeatureApi, LogTagProvider {
    override val TAG: String = "FFinishSetupFeatureApi"

    private data class TasksDependencies(
        val bleStatus: FBleStatus?,
        val linkedAccountInfo: LinkedAccountInfo,
        val wifiStatus: BsbWifiStatus,
        val updateVersion: BsbUpdateVersion,
        val isSetupFinishedBefore: Boolean
    )

    private fun createPairBleTask(bleStatus: FBleStatus): DeviceSetupTask {
        val deviceSetupTask = DeviceSetupTask(
            type = DeviceSetupTaskType.PAIR_BLE,
            status = when {
                bleStatus is FBleStatus.Connected -> DeviceSetupTaskStatus.COMPLETED
                else -> DeviceSetupTaskStatus.NOT_COMPLETED
            },
        )
        verbose { "#createPairBleTask $bleStatus -> $deviceSetupTask" }
        return deviceSetupTask
    }

    private fun createConnectWifiTask(wifiStatus: BsbWifiStatus): DeviceSetupTask {
        val deviceSetupTask = DeviceSetupTask(
            type = DeviceSetupTaskType.CONNECT_WIFI,
            status = when (wifiStatus) {
                is BsbWifiStatus.Connected -> DeviceSetupTaskStatus.COMPLETED
                BsbWifiStatus.Disconnected -> DeviceSetupTaskStatus.NOT_COMPLETED
                BsbWifiStatus.Connecting,
                BsbWifiStatus.Disconnecting,
                BsbWifiStatus.Reconnecting,
                BsbWifiStatus.Unknown -> DeviceSetupTaskStatus.NOT_AVAILABLE
            },
        )
        verbose { "#createConnectWifiTask $wifiStatus -> $deviceSetupTask" }
        return deviceSetupTask
    }

    private fun createLinkAccountTask(
        linkedAccountInfo: LinkedAccountInfo,
        connectWifiTaskStatus: DeviceSetupTaskStatus
    ): DeviceSetupTask {
        val deviceSetupTask = DeviceSetupTask(
            type = DeviceSetupTaskType.LINK_ACCOUNT,
            status = when (linkedAccountInfo) {
                is LinkedAccountInfo.Linked.SameUser -> DeviceSetupTaskStatus.COMPLETED
                else -> when (connectWifiTaskStatus) {
                    DeviceSetupTaskStatus.COMPLETED -> DeviceSetupTaskStatus.NOT_COMPLETED
                    DeviceSetupTaskStatus.NOT_COMPLETED -> DeviceSetupTaskStatus.NOT_AVAILABLE
                    DeviceSetupTaskStatus.LOADING,
                    DeviceSetupTaskStatus.NOT_AVAILABLE -> DeviceSetupTaskStatus.NOT_AVAILABLE
                }
            }
        )
        verbose { "#createLinkAccountTask $linkedAccountInfo; $connectWifiTaskStatus -> $deviceSetupTask" }
        return deviceSetupTask
    }

    private fun createUpdateFirmwareTask(
        updateVersion: BsbUpdateVersion,
        connectWifiTaskStatus: DeviceSetupTaskStatus
    ): DeviceSetupTask {
        val deviceSetupTask = DeviceSetupTask(
            type = DeviceSetupTaskType.UPDATE_FIRMWARE,
            status = when (connectWifiTaskStatus) {
                DeviceSetupTaskStatus.COMPLETED -> {
                    when (updateVersion) {
                        BsbUpdateVersion.FailedToCheck -> DeviceSetupTaskStatus.NOT_AVAILABLE
                        BsbUpdateVersion.CheckingOnBBInProgress,
                        BsbUpdateVersion.Loading -> DeviceSetupTaskStatus.LOADING
                        BsbUpdateVersion.NoUpdateAvailable -> DeviceSetupTaskStatus.COMPLETED
                        is BsbUpdateVersion.ReadyToUpdate -> DeviceSetupTaskStatus.NOT_COMPLETED
                    }
                }

                DeviceSetupTaskStatus.NOT_COMPLETED -> DeviceSetupTaskStatus.NOT_AVAILABLE
                DeviceSetupTaskStatus.LOADING,
                DeviceSetupTaskStatus.NOT_AVAILABLE -> DeviceSetupTaskStatus.COMPLETED
            },
        )
        verbose { "#createUpdateFirmwareTask $updateVersion; $connectWifiTaskStatus -> $deviceSetupTask" }
        return deviceSetupTask
    }

    override val taskListResourceFlow: WrappedSharedFlow<FFinishSetupState> = combine(
        flow = fBleFeatureApi?.getBleStatus().orNullable()
            .distinctUntilChanged(),
        flow2 = fLinkedInfoOnDemandFeatureApi.status
            .distinctUntilChanged(),
        flow3 = fWiFiFeatureApi.getWifiStatusFlow()
            .distinctUntilChanged(),
        flow4 = fFirmwareUpdateFeatureApi.updateVersionFlow
            .map { keyedVersion -> keyedVersion.version },
        flow5 = setupFinishedBeforeKrate.cachedStateFlow,
        transform = ::TasksDependencies
    ).transformWhileSubscribed(
        timeout = 5.seconds,
        scope = scope,
        transformFlow = { flow ->
            verbose { "#taskListResourceFlow transformFlow" }
            flow.throttleLatest { tasksDependencies ->
                val bleStatus = tasksDependencies.bleStatus
                val linkedAccountInfo = tasksDependencies.linkedAccountInfo
                val wifiStatus = tasksDependencies.wifiStatus
                val updateVersion = tasksDependencies.updateVersion
                val isSetupFinishedBefore = tasksDependencies.isSetupFinishedBefore

                if (isSetupFinishedBefore) {
                    verbose { "#taskListResourceFlow isSetupFinishedBefore: $isSetupFinishedBefore" }
                    return@throttleLatest FFinishSetupState.FinishedBefore
                }
                if (fBleFeatureApi != null && bleStatus == null) {
                    return@throttleLatest FFinishSetupState.Loading
                }
                if (fBleFeatureApi != null && bleStatus is FBleStatus.Initialization) {
                    verbose { "#taskListResourceFlow BLE is not initialized" }
                    return@throttleLatest FFinishSetupState.Loading
                }
                when (wifiStatus) {
                    BsbWifiStatus.Connecting,
                    BsbWifiStatus.Reconnecting -> {
                        verbose { "#taskListResourceFlow WIFI not initialized yet" }
                        return@throttleLatest FFinishSetupState.Loading
                    }
                    is BsbWifiStatus.Connected,
                    BsbWifiStatus.Disconnected,
                    BsbWifiStatus.Disconnecting,
                    BsbWifiStatus.Unknown -> Unit
                }

                val pairBleTask = bleStatus?.let(::createPairBleTask)
                val connectWifiTask = createConnectWifiTask(wifiStatus)
                val linkAccountTask = createLinkAccountTask(
                    linkedAccountInfo = linkedAccountInfo,
                    connectWifiTaskStatus = connectWifiTask.status
                )
                val updateFirmwareTask = createUpdateFirmwareTask(
                    updateVersion = updateVersion,
                    connectWifiTaskStatus = connectWifiTask.status
                )
                val tasks = listOfNotNull(
                    pairBleTask,
                    connectWifiTask,
                    linkAccountTask,
                    updateFirmwareTask
                )
                val isAllCompleted = tasks.all { deviceSetupTask ->
                    deviceSetupTask.status == DeviceSetupTaskStatus.COMPLETED
                }
                if (isAllCompleted) {
                    verbose { "#taskListResourceFlow all tasks completed" }
                    setupFinishedBeforeKrate.save(true)
                    FFinishSetupState.FinishedBefore
                } else {
                    FFinishSetupState.Loaded(tasks)
                }
            }
        }
    ).wrap()

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.FINISH_SETUP)
    class FDeviceFeatureApiFactory(
        private val setupFinishedBeforeKrate: SetupFinishedBeforeKrate
    ) : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fBleFeatureApi = when (BusyLibPlatform.currentPlatform) {
                BusyLibPlatform.ANDROID, BusyLibPlatform.IOS -> {
                    unsafeFeatureDeviceApi
                        .get(FBleFeatureApi::class)
                        ?.await()
                        ?: return null
                }

                BusyLibPlatform.JVM,
                BusyLibPlatform.MACOS -> null
            }
            val fLinkedInfoOnDemandFeatureApi = unsafeFeatureDeviceApi
                .get(FLinkedInfoOnDemandFeatureApi::class)
                ?.await()
                ?: return null
            val fWiFiFeatureApi = unsafeFeatureDeviceApi
                .get(FWiFiFeatureApi::class)
                ?.await()
                ?: return null

            val fFirmwareUpdateFeatureApi = unsafeFeatureDeviceApi
                .get(FFirmwareUpdateFeatureApi::class)
                ?.await()
                ?: return null

            return FFinishSetupFeatureApiImpl(
                scope = scope,
                fBleFeatureApi = fBleFeatureApi,
                fLinkedInfoOnDemandFeatureApi = fLinkedInfoOnDemandFeatureApi,
                fWiFiFeatureApi = fWiFiFeatureApi,
                fFirmwareUpdateFeatureApi = fFirmwareUpdateFeatureApi,
                setupFinishedBeforeKrate = setupFinishedBeforeKrate
            )
        }
    }
}
