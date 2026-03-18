package net.flipper.bsb.watchers.provisioning

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.uuid.Uuid

private const val DEFAULT_BUSY_BAR_NAME = "BUSY Bar"

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class CloudFetcherWatcher(
    scope: CoroutineScope,
    private val persistedStorage: FDevicePersistedStorage,
    private val busyLibNetworkStateApi: BUSYLibNetworkStateApi,
    private val principalApi: BUSYLibPrincipalApi,
    private val busyCloudBarsApi: BusyCloudBarsApi,
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "CloudFetcherWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            combine(
                persistedStorage.getAllDevicesFlow()
                    .distinctUntilChanged(),
                busyLibNetworkStateApi.isNetworkAvailableFlow,
                principalApi.getPrincipalFlow()
            ) { allDevices, isNetworkAvailable, principal ->
                Triple(
                    allDevices.mapNotNull {
                        it.cloud
                    }.map { it.deviceId },
                    isNetworkAvailable,
                    principal
                )
            }.collectLatest { (localCloudDeviceIds, isNetworkAvailable, principal) ->
                val cloudBars = if (isNetworkAvailable && principal is BUSYLibUserPrincipal.Token) {
                    busyCloudBarsApi.getBarsList(principal)
                        .onFailure {
                            error(it) { "Failed to get bars list" }
                        }.getOrNull()
                } else {
                    null
                }
                if (cloudBars == null) {
                    debug { "Skip synchronization because busy bar list is null" }
                    return@collectLatest
                }
                val cloudBarsId = cloudBars.mapNotNull { Uuid.parseOrNull(it.id) }
                if (localCloudDeviceIds.sorted() == cloudBarsId.sorted()) {
                    info { "Cloud devices and local are same, skip invalidation" }
                } else {
                    info { "Found difference between cloud and local devices, start invalidation" }
                    persistedStorage.transaction {
                        invalidateCloudBars(cloudBars)
                    }
                }
            }
        }
    }

    private fun PersistedStorageTransactionScope.invalidateCloudBars(
        cloudBars: List<BusyCloudBar>
    ) {
        val cloudBarsId = cloudBars.mapNotNull { Uuid.parseOrNull(it.id) }.toSet()
        val deviceClouds = getAllDevices().mapNotNull { device ->
            val deviceId = device.cloud?.deviceId ?: return@mapNotNull null
            device to deviceId
        }
        val deviceCloudIds = deviceClouds.map { (_, deviceId) -> deviceId }.toSet()

        // Remove unlinked
        deviceClouds.filterNot { (_, deviceId) -> deviceId in cloudBarsId }.forEach { (device, _) ->
            removeCloud(device)
        }

        // Add new link
        cloudBars.mapNotNull { Uuid.parseOrNull(it.id)?.let { id -> id to it } }
            .filterNot { (id, _) -> id in deviceCloudIds }
            .forEach { (id, bar) ->
                info { "Add new bar: $bar" }
                val newBusyBar = BUSYBar(
                    humanReadableName = bar.label ?: DEFAULT_BUSY_BAR_NAME,
                    cloud = BUSYBar.ConnectionWay.Cloud(deviceId = id)
                )
                addOrReplace(newBusyBar)
            }
    }

    private fun PersistedStorageTransactionScope.removeCloud(
        device: BUSYBar
    ) {
        val withoutCloud = device.copy(cloud = null)
        if (withoutCloud.connectionWays.isEmpty()) {
            info { "Remove $device, because cloud not found in linked device" }
            removeDevice(device.uniqueId)
        } else {
            info { "Remove cloud link from $device, new connections: ${withoutCloud.connectionWays}" }
            addOrReplace(withoutCloud)
        }
    }
}
