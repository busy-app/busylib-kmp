package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.getDevice
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.config.api.model.copyTransports
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.feature.hardwareid.api.FHardwareIdFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.uuid.Uuid

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class HardwareIdProvisioningWatcher(
    scope: CoroutineScope,
    private val featureProvider: FFeatureProvider,
    private val orchestrator: FDeviceOrchestrator,
    private val persistedStorage: FInternalDevicePersistedStorage,
    private val cloudInvalidator: CloudInvalidator
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "HardwareIdProvisioningWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            orchestrator.getState().flatMapLatest { state ->
                if (state is FDeviceConnectStatus.Connected) {
                    featureProvider.get<FHardwareIdFeatureApi>().map { it to state.device }
                } else {
                    flowOf()
                }
            }.collectLatest { (featureStatus, device) ->
                when (featureStatus) {
                    FFeatureStatus.NotFound,
                    FFeatureStatus.Retrieving,
                    FFeatureStatus.Unsupported -> {
                    } // Nothing

                    is FFeatureStatus.Supported<FHardwareIdFeatureApi> -> {
                        featureStatus.featureApi.getHardwareIdFlow()
                            .filterNotNull()
                            .collectLatest { hardwareId ->
                                onNewHardwareId(
                                    hardwareId = hardwareId,
                                    uniqueId = device.uniqueId
                                )
                            }
                    }
                }
            }
        }
    }

    private suspend fun onNewHardwareId(hardwareId: String, uniqueId: String) {
        val shouldInvalidateCloud = persistedStorage.transactionInternal {
            return@transactionInternal getDevice(uniqueId)?.let { original ->
                if (original.hardwareId == null) {
                    info { "Found device without hardware id ($hardwareId): $original" }
                    addOrReplace(
                        original.copy(
                            hardwareId = hardwareId
                        )
                    )
                    return@let false
                } else if (original.hardwareId != hardwareId) {
                    info {
                        "Found device with another hardware id. " +
                            "Current is $original, but new one is $hardwareId"
                    }
                    val existedDevice = getAllDevices().find { it.hardwareId == hardwareId }
                    val newCurrentDevice = if (existedDevice == null) {
                        original.copyTransports(uniqueId = Uuid.random().toString())
                            .copy(
                                hardwareId = hardwareId
                            ).copy(cloud = null)
                    } else {
                        info { "Found existed device with hardware id" }
                        existedDevice
                    }
                    info { "New device is: $newCurrentDevice" }
                    // Add device if not and set it
                    newCurrentDevice?.let { setCurrentDevice(it) }
                    return@let true
                } else {
                    return@let false
                }
            } ?: false
        }
        if (shouldInvalidateCloud) {
            cloudInvalidator.invalidate()
        }
    }
}
