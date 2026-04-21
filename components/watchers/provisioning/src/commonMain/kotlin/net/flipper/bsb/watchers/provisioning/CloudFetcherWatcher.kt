package net.flipper.bsb.watchers.provisioning

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
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
    private val persistedStorage: FInternalDevicePersistedStorage,
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
                Triple(allDevices, isNetworkAvailable, principal)
            }.collectLatest { (allDevices, isNetworkAvailable, principal) ->
                val cloudBars = when (principal) {
                    BUSYLibUserPrincipal.Empty -> emptyList()
                    BUSYLibUserPrincipal.Loading -> null
                    is BUSYLibUserPrincipal.Token -> if (isNetworkAvailable) {
                        busyCloudBarsApi.getBarsList(principal)
                            .onFailure {
                                error(it) { "Failed to get bars list" }
                            }.getOrNull()
                    } else {
                        null
                    }
                }
                if (cloudBars == null) {
                    debug { "Skip synchronization because busy bar list is null" }
                    return@collectLatest
                }
                val localCloudDeviceIds = allDevices.mapNotNull { it.cloud?.deviceId }
                val cloudBarsId = cloudBars.mapNotNull { Uuid.parseOrNull(it.id) }
                val idsMatch = localCloudDeviceIds.sorted() == cloudBarsId.sorted()
                val hasNameMismatch = hasCloudLabelMismatch(allDevices, cloudBars)
                if (idsMatch && !hasNameMismatch) {
                    info { "Cloud devices and local are same, skip invalidation" }
                } else {
                    info { "Found difference between cloud and local devices, start invalidation" }
                    persistedStorage.transactionInternal {
                        invalidateCloudBars(cloudBars)
                    }
                }
            }
        }
    }

    private fun hasCloudLabelMismatch(
        allDevices: List<BUSYBar>,
        cloudBars: List<BusyCloudBar>
    ): Boolean {
        val cloudBarsById = cloudBars.cloudBarsById()
        return allDevices.any { device ->
            val id = device.cloud?.deviceId ?: return@any false
            val label = cloudBarsById[id]?.label ?: return@any false
            label != device.humanReadableName
        }
    }

    private fun InternalStorageTransactionScope.invalidateCloudBars(
        cloudBars: List<BusyCloudBar>
    ) {
        val cloudBarsById = cloudBars.cloudBarsById()
        val cloudBarsId = cloudBarsById.keys
        val deviceClouds = getAllDevices().mapNotNull { device ->
            val deviceId = device.cloud?.deviceId ?: return@mapNotNull null
            device to deviceId
        }
        val deviceCloudIds = deviceClouds.map { (_, deviceId) -> deviceId }.toSet()

        // Remove unlinked
        deviceClouds.filterNot { (_, deviceId) -> deviceId in cloudBarsId }.forEach { (device, _) ->
            removeCloud(device)
        }

        // Rename still-linked devices when cloud label changed
        deviceClouds.filter { (_, deviceId) -> deviceId in cloudBarsId }
            .forEach { (device, deviceId) ->
                val label = cloudBarsById[deviceId]?.label ?: return@forEach
                if (device.humanReadableName != label) {
                    info {
                        "Rename ${device.uniqueId}: '${device.humanReadableName}' -> '$label'"
                    }
                    addOrReplace(device.copy(humanReadableName = label))
                }
            }

        // Add new link
        cloudBarsById.filterKeys { id -> id !in deviceCloudIds }
            .forEach { (id, bar) ->
                info { "Add new bar: $bar" }
                val newBusyBar = BUSYBar(
                    humanReadableName = bar.label ?: DEFAULT_BUSY_BAR_NAME,
                    cloud = BUSYBar.ConnectionWay.Cloud(deviceId = id)
                )
                addOrReplace(newBusyBar)
            }
    }

    private fun List<BusyCloudBar>.cloudBarsById(): Map<Uuid, BusyCloudBar> =
        mapNotNull { bar -> Uuid.parseOrNull(bar.id)?.let { id -> id to bar } }.toMap()

    private fun InternalStorageTransactionScope.removeCloud(
        device: BUSYBar
    ) {
        val withoutCloud = device.copy(cloud = null)
        if (withoutCloud == null) {
            info { "Remove $device, because cloud not found in linked device" }
            removeDevice(device.uniqueId)
        } else {
            info { "Remove cloud link from $device, new connections: ${withoutCloud.connectionWays}" }
            addOrReplace(withoutCloud)
        }
    }
}
