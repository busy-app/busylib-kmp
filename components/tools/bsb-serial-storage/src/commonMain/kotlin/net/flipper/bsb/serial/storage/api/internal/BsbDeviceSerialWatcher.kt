package net.flipper.bsb.serial.storage.api.internal

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getFilteredFeature
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.launchIn
import net.flipper.core.busylib.ktx.common.onLatest
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

/**
 * Learns the serial number of every connected BUSY Bar and persists it in
 * [BsbDeviceSerialStoreImpl]. The serial identifies the on-disk data of a bar
 * (for example its Draw tool collection directory), so once it is learned
 * that data can be resolved fully offline. Serials never change, so an
 * already known device is skipped without an RPC round trip.
 */
@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<InternalBUSYLibStartupListener>())
class BsbDeviceSerialWatcher(
    scope: CoroutineScope,
    private val featureProvider: FFeatureProvider,
    private val orchestrator: FDeviceOrchestrator,
    private val serialStore: BsbDeviceSerialStoreImpl
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "BsbDeviceSerialWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    private suspend fun rememberDeviceSerial(
        rpcFeatureApi: FRpcFeatureApi,
        deviceUniqueId: String
    ) {
        val knownSerial = serialStore.findSerial(deviceUniqueId)
        if (knownSerial != null) return
        runSuspendCatching {
            exponentialRetry(retries = SERIAL_REQUEST_RETRIES) {
                rpcFeatureApi.fRpcSystemApi.getDeviceStatus()
            }
        }.onSuccess { deviceStatus ->
            serialStore.rememberSerial(deviceUniqueId, deviceStatus.serialNumber)
            info { "Learned serial ${deviceStatus.serialNumber} of device $deviceUniqueId" }
        }.onFailure { learnError ->
            error(learnError) { "Failed to learn the serial of device $deviceUniqueId" }
        }
    }

    override fun onLaunch() {
        info { "#onLaunch" }
        orchestrator.getState()
            .flatMapLatest { connectStatus ->
                featureProvider.getFilteredFeature<FRpcFeatureApi>(connectStatus)
            }
            .filterNotNull()
            .onLatest { (rpcFeatureApi, connectedState) ->
                rememberDeviceSerial(
                    rpcFeatureApi = rpcFeatureApi,
                    deviceUniqueId = connectedState.device.uniqueId
                )
            }
            .launchIn(singleJobScope, SingleJobMode.CANCEL_PREVIOUS)
    }

    companion object {
        private const val SERIAL_REQUEST_RETRIES = 10L
    }
}
