package net.flipper.bridge.connection.feature.finishsetup.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.ble.api.FBleFeatureApi
import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.finishsetup.krate.SetupFinishedBeforeKrate
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTask
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTaskStatus
import net.flipper.bridge.connection.feature.finishsetup.model.DeviceSetupTaskType
import net.flipper.bridge.connection.feature.finishsetup.model.FFinishSetupState
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration.Companion.seconds

class FFinishSetupFeatureApiImpl(
    private val scope: CoroutineScope,
    private val fBleFeatureApi: FBleFeatureApi,
    private val fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi,
    private val fWiFiFeatureApi: FWiFiFeatureApi,
    private val fFirmwareUpdateFeatureApi: FFirmwareUpdateFeatureApi,
    private val setupFinishedBeforeKrate: SetupFinishedBeforeKrate
) : FFinishSetupFeatureApi, LogTagProvider {
    override val TAG: String = "FFinishSetupFeatureApi"

    private data class TasksDependencies(
        val bleStatus: FBleStatus,
        val linkedAccountInfo: LinkedAccountInfo,
        val wifiStatus: StatusResponse,
        val updateStatus: UpdateStatus,
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

    private fun createConnectWifiTask(wifiStatus: StatusResponse): DeviceSetupTask {
        val deviceSetupTask = DeviceSetupTask(
            type = DeviceSetupTaskType.CONNECT_WIFI,
            status = when (wifiStatus.state) {
                StatusResponse.State.CONNECTED -> DeviceSetupTaskStatus.COMPLETED
                StatusResponse.State.DISCONNECTED -> DeviceSetupTaskStatus.NOT_COMPLETED

                StatusResponse.State.CONNECTING,
                StatusResponse.State.RECONNECTING,
                StatusResponse.State.DISCONNECTING,
                StatusResponse.State.UNKNOWN -> DeviceSetupTaskStatus.NOT_AVAILABLE
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
        updateStatus: UpdateStatus,
        connectWifiTaskStatus: DeviceSetupTaskStatus
    ): DeviceSetupTask {
        val deviceSetupTask = DeviceSetupTask(
            type = DeviceSetupTaskType.UPDATE_FIRMWARE,
            status = when (connectWifiTaskStatus) {
                DeviceSetupTaskStatus.COMPLETED -> {
                    when (updateStatus.check.status) {
                        UpdateStatus.Check.CheckResult.AVAILABLE -> DeviceSetupTaskStatus.NOT_COMPLETED
                        UpdateStatus.Check.CheckResult.NOT_AVAILABLE -> DeviceSetupTaskStatus.NOT_AVAILABLE
                        UpdateStatus.Check.CheckResult.FAILURE -> DeviceSetupTaskStatus.NOT_AVAILABLE
                        UpdateStatus.Check.CheckResult.NONE -> DeviceSetupTaskStatus.LOADING
                    }
                }

                DeviceSetupTaskStatus.NOT_COMPLETED -> DeviceSetupTaskStatus.NOT_AVAILABLE
                DeviceSetupTaskStatus.LOADING,
                DeviceSetupTaskStatus.NOT_AVAILABLE -> DeviceSetupTaskStatus.NOT_AVAILABLE
            },
        )
        verbose { "#createUpdateFirmwareTask $updateStatus; $connectWifiTaskStatus -> $deviceSetupTask" }
        return deviceSetupTask
    }

    override val taskListResourceFlow: WrappedSharedFlow<FFinishSetupState> = combine(
        flow = fBleFeatureApi.getBleStatus(),
        flow2 = fLinkedInfoOnDemandFeatureApi.status,
        flow3 = fWiFiFeatureApi.getWifiStatusFlow(),
        flow4 = fFirmwareUpdateFeatureApi.getUpdateStatusFlow(),
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
                val updateStatus = tasksDependencies.updateStatus
                val isSetupFinishedBefore = tasksDependencies.isSetupFinishedBefore

                if (isSetupFinishedBefore) {
                    verbose { "#taskListResourceFlow isSetupFinishedBefore: $isSetupFinishedBefore" }
                    return@throttleLatest FFinishSetupState.FinishedBefore
                }

                if (bleStatus == null && linkedAccountInfo == null && wifiStatus == null) {
                    return@throttleLatest FFinishSetupState.Loading
                }
                if (bleStatus == null || linkedAccountInfo == null || wifiStatus == null) {
                    return@throttleLatest FFinishSetupState.Loading
                }
                if (bleStatus is FBleStatus.Initialization) {
                    verbose { "#taskListResourceFlow BLE is not initialized" }
                    return@throttleLatest FFinishSetupState.Loading
                }
                when (wifiStatus.state) {
                    StatusResponse.State.RECONNECTING,
                    StatusResponse.State.CONNECTING -> {
                        verbose { "#taskListResourceFlow WIFI not initialized yet" }
                        return@throttleLatest FFinishSetupState.Loading
                    }

                    StatusResponse.State.DISCONNECTED,
                    StatusResponse.State.CONNECTED,
                    StatusResponse.State.DISCONNECTING,
                    StatusResponse.State.UNKNOWN -> Unit
                }

                val pairBleTask = createPairBleTask(bleStatus)
                val connectWifiTask = createConnectWifiTask(wifiStatus)
                val linkAccountTask = createLinkAccountTask(
                    linkedAccountInfo = linkedAccountInfo,
                    connectWifiTaskStatus = connectWifiTask.status
                )
                val updateFirmwareTask = createUpdateFirmwareTask(
                    updateStatus = updateStatus,
                    connectWifiTaskStatus = connectWifiTask.status
                )
                val tasks = listOf(
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
    class FDeviceFeatureApiFactory(
        private val setupFinishedBeforeKrate: SetupFinishedBeforeKrate
    ) : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fBleFeatureApi = unsafeFeatureDeviceApi
                .get(FBleFeatureApi::class)
                ?.await()
                ?: return null
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

    @ContributesTo(BusyLibGraph::class)
    interface FBleFeatureComponent {
        @Provides
        @IntoMap
        fun provideFBleFeatureFactory(
            fBleFeatureFactory: FDeviceFeatureApiFactory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.FINISH_SETUP to fBleFeatureFactory
        }
    }
}
