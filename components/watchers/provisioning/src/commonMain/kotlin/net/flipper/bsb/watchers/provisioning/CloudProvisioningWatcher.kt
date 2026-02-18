package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.wtf
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.uuid.Uuid


@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class CloudProvisioningWatcher(
    scope: CoroutineScope,
    private val featureProvider: FFeatureProvider,
    private val orchestrator: FDeviceOrchestrator,
    private val persistedStorage: FDevicePersistedStorage
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "CloudProvisioningWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch {
            combine(
                orchestrator.getState(),
                featureProvider.get<FRpcCriticalFeatureApi>()
            ) { state, rpcApiStatus ->
                if (state is FDeviceConnectStatus.Connected) {
                    when (rpcApiStatus) {
                        FFeatureStatus.NotFound,
                        FFeatureStatus.Retrieving,
                        FFeatureStatus.Unsupported -> flowOf()

                        is FFeatureStatus.Supported<FRpcCriticalFeatureApi> -> {
                            rpcApiStatus.featureApi.currentAccountInfo.map { it to state.device.uniqueId }
                        }
                    }
                } else {
                    flowOf()
                }
            }.flatMapLatest { it }
                .collectLatest { (linkedInfo, deviceId) ->
                    runSuspendCatching {
                        onNewLinkedInfo(linkedInfo, deviceId)
                    }.onFailure {
                        error(it) { "Failed to handle new linked info" }
                    }
                }
        }
    }

    private suspend fun onNewLinkedInfo(linkedInfo: RpcLinkedAccountInfo?, deviceId: String) {
        val cloudId = linkedInfo?.cloudId?.let(Uuid::parse) ?: return
        persistedStorage.updateDevice(deviceId) { device ->
            getNewBUSYBar(cloudId, device)
        }
    }

    private fun getNewBUSYBar(cloudId: Uuid, device: BUSYBar): BUSYBar {
        val cloudConnection = device
            .connectionWays
            .filterIsInstance<BUSYBar.ConnectionWay.Cloud>().firstOrNull()
        if (cloudConnection != null) {
            if (cloudConnection.deviceId == cloudId) {
                return device
            } else {
                wtf {
                    "For device $device linked to cloud with id $cloudId, " +
                            "but current connection is with " +
                            "device with id ${cloudConnection.deviceId}"
                }
            }
        }
        info { "Found new cloud connection for device $device with id $cloudId" }
        val newConnections = device.connectionWays
            .filter { it !is BUSYBar.ConnectionWay.Cloud }
            .plus(BUSYBar.ConnectionWay.Cloud(cloudId))

        return device.copy(
            connectionWays = newConnections.sortedByDescending { it.getPriority() }
        )
    }
}

private fun BUSYBar.ConnectionWay.getPriority(): Int {
    return when (this) {
        is BUSYBar.ConnectionWay.BLE -> 0 // Slowest transport, lowest priority
        is BUSYBar.ConnectionWay.Cloud -> 10 // Faster than ble
        is BUSYBar.ConnectionWay.Lan -> 100 // Fastest transport
        BUSYBar.ConnectionWay.Mock -> -1 // Use only for debug
    }
}