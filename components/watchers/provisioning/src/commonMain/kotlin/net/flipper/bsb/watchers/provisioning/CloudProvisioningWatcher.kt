package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.getDevice
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.config.api.model.copyTransports
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusDevice
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.uuid.Uuid

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class CloudProvisioningWatcher(
    scope: CoroutineScope,
    private val featureProvider: FFeatureProvider,
    private val orchestrator: FDeviceOrchestrator,
    private val persistedStorage: FInternalDevicePersistedStorage
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "CloudProvisioningWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            orchestrator.getState().flatMapLatest { state ->
                if (state is FDeviceConnectStatus.Connected && state.device.hardwareId == null) {
                    featureProvider.get<FRpcFeatureApi>().map { it to state.device }
                } else flowOf()
            }.collectLatest { (rpcApiStatus, device) ->
                when (rpcApiStatus) {
                    FFeatureStatus.NotFound,
                    FFeatureStatus.Retrieving,
                    FFeatureStatus.Unsupported -> {
                    } // Nothing

                    is FFeatureStatus.Supported<FRpcFeatureApi> -> {
                        rpcApiStatus.featureApi.fRpcSystemApi.getDeviceStatus()
                            .onFailure {
                                error(it) { "Failed to get system info" }
                            }.onSuccess { deviceStatus ->
                                onNewDeviceStatus(
                                    deviceStatus = deviceStatus,
                                    device = device
                                )
                            }
                    }
                }
            }
        }
    }

    private suspend fun onNewDeviceStatus(deviceStatus: BusyBarStatusDevice, device: BUSYBar) {
        persistedStorage.transactionInternal {
            getDevice(device.uniqueId)?.let {
                updateBUSYBar(cloudId, it)
            }
        }
    }

    /**
     * Can be this options:
     * - Only local transport, connected to cloud - add cloud transport to current device
     * - Only local transport, not connected to cloud - skip
     * - Local and cloud transport, cloud linked to current device - skip
     * - Local and cloud transport, cloud linked to different device - add
     *          new device with cloud device (if not exist already) and switched to it
     * - Local and cloud, not connected to cloud - remove cloud connection
     */
    private fun InternalStorageTransactionScope.updateBUSYBar(cloudId: Uuid?, original: BUSYBar) {
        val cloudConnection = original.cloud
        if (cloudConnection == null) {
            if (cloudId != null) { // Only local transport, connected to cloud - add cloud transport to current device
                info { "Found new cloud connection for device $original with id $cloudId" }
                modifyOrDelete(original) {
                    it.copy(cloud = BUSYBar.ConnectionWay.Cloud(cloudId))
                }
                return
            }
            // Only local transport, not connected to cloud - skip
            return
        }
        // cloudConnection != null
        if (cloudConnection.deviceId == cloudId) {
            return // Local and cloud transport, cloud linked to current device - skip
        }

        if (cloudId == null) { // Local and cloud, not connected to cloud - remove cloud connection
            modifyOrDelete(original) {
                it.copy(cloud = null)
            }
            return
        }

        // Cloud connection exist, but different. It can be when connected by LAN/USB two BUSY Bars
        // Local and cloud transport, cloud linked to different device - add
        //      new device with cloud device (if not exist already) and switched to it
        info {
            "Found new cloud connection for device $original, " +
                    "but it is already connected to cloud with id $cloudId"
        }
        val allDevices = getAllDevices()
        val existedDevice = allDevices.find { deviceFromStorage ->
            deviceFromStorage.cloud?.deviceId == cloudId
        }
        if (existedDevice != null) {
            info { "Found existed device connected to cloud with id $cloudId" }
            setCurrentDevice(existedDevice)
            return
        }
        info { "Create new device for cloud connection with id $cloudId" }
        val newBUSYBar = original.copyTransports(
            uniqueId = Uuid.random().toString(),
        ).addTransport(
            cloud = BUSYBar.ConnectionWay.Cloud(cloudId)
        )
        addOrReplace(newBUSYBar)
        setCurrentDevice(newBUSYBar)
    }
}

private fun InternalStorageTransactionScope.modifyOrDelete(
    original: BUSYBar,
    block: (BUSYBar) -> BUSYBar?
) {
    val result = block(original)
    if (result == null) {
        removeDevice(original.uniqueId)
    } else {
        addOrReplace(result)
    }
}
