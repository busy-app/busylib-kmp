package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.getDevice
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getFilteredFeature
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusDevice
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class HardwareIdProvisioningWatcher(
    scope: CoroutineScope,
    private val featureProvider: FFeatureProvider,
    private val orchestrator: FDeviceOrchestrator,
    private val persistedStorage: FInternalDevicePersistedStorage
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "HardwareIdProvisioningWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            orchestrator.getState().flatMapLatest {
                featureProvider.getFilteredFeature<FRpcFeatureApi>(it)
            }.filterNotNull().mapNotNull { (featureApi, state) ->
                if (state.device.hardwareId == null) {
                    featureApi to state.device
                } else {
                    null
                }
            }.collectLatest { (featureApi, device) ->
                featureApi.fRpcSystemApi.getDeviceStatus()
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

    private suspend fun onNewDeviceStatus(deviceStatus: BusyBarStatusDevice, device: BUSYBar) {
        persistedStorage.transactionInternal {
            getDevice(device.uniqueId)?.let { original ->
                addOrReplace(
                    original.copy(
                        hardwareId = deviceStatus.serialNumber
                    )
                )
            }
        }
    }
}
