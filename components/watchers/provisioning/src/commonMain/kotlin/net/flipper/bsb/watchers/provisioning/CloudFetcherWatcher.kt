package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.bsb.watchers.provisioning.utils.CloudFetcher
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.uuid.Uuid

private const val DEFAULT_BUSY_BAR_NAME = "BUSY Bar"

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class CloudFetcherWatcher(
    scope: CoroutineScope,
    private val persistedStorage: FInternalDevicePersistedStorage,
    private val cloudFetcher: CloudFetcher,
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "CloudFetcherWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            combine(
                persistedStorage.getAllDevicesFlow()
                    .distinctUntilChanged(),
                cloudFetcher.getBarsFlow()
            ) { allDevices, cloudBars ->
                allDevices to cloudBars
            }.collectLatest { (allDevices, cloudBars) ->
                if (!hasMismatch(allDevices, cloudBars)) {
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

    private fun hasMismatch(allDevices: List<BUSYBar>, cloudBars: List<BusyCloudBar>): Boolean {
        val localCloudIds = allDevices.mapNotNull { it.cloud?.deviceId }.sorted()
        if (localCloudIds != cloudBars.map { it.id }.sorted()) {
            return true
        }
        val localCloudIdsSet = allDevices.associateBy { it.cloud?.deviceId }
        cloudBars.forEach { device ->
            val local = localCloudIdsSet[device.id]
            if (device.label != null && local?.humanReadableName == DEFAULT_BUSY_BAR_NAME) {
                if (device.label != local.humanReadableName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * If cloud id locally and no in cloud - remove this busybar
     * If in cloud exist:
     * - Find by cloudId locally or hardware id -> merge it
     * - If no local, add them
     */
    private fun InternalStorageTransactionScope.invalidateCloudBars(
        cloudBars: List<BusyCloudBar>
    ) {
        val cloudBarsById = cloudBars.associateBy { it.id }
        val cloudBarsId = cloudBarsById.keys

        removeUnlinked(cloudBarsId)
        val toAdd = mergeExisting(cloudBars)
        addNew(toAdd)
    }

    private fun InternalStorageTransactionScope.addNew(toAdd: List<BusyCloudBar>) {
        toAdd.forEach { cloud ->
            info { "Add new bar: $cloud" }
            addOrReplace(
                BUSYBar(
                    humanReadableName = cloud.label ?: DEFAULT_BUSY_BAR_NAME,
                    hardwareId = cloud.hardwareId,
                    cloud = BUSYBar.ConnectionWay.Cloud(deviceId = cloud.id)
                )
            )
        }
    }

    private fun InternalStorageTransactionScope.mergeExisting(
        cloudBars: List<BusyCloudBar>
    ): List<BusyCloudBar> {
        val localByCloudId = getAllDevices().associateBy { it.cloud?.deviceId }
        val localByHardwareId = getAllDevices().associateBy { it.hardwareId }
        val toAdd = cloudBars.toMutableSet()

        cloudBars.forEach { cloud ->
            val local = localByCloudId[cloud.id] ?: localByHardwareId[cloud.hardwareId]
            if (local != null) {
                if (local.hardwareId != cloud.hardwareId) {
                    warn { "Hardware id mismatch: $local vs $cloud" }
                }
                info { "Update existing busy bar $local" }
                val newName = if (local.humanReadableName == DEFAULT_BUSY_BAR_NAME) {
                    cloud.label ?: local.humanReadableName
                } else local.humanReadableName
                addOrReplace(
                    local.copy(
                        humanReadableName = newName,
                        hardwareId = cloud.hardwareId
                    ).addTransport(cloud = BUSYBar.ConnectionWay.Cloud(deviceId = cloud.id))
                )
                toAdd.remove(cloud)
            }
        }
        return toAdd.toList()
    }

    private fun InternalStorageTransactionScope.removeUnlinked(cloudBarsId: Set<Uuid>) {
        val localDevices = getAllDevices().mapNotNull { device ->
            val deviceId = device.cloud?.deviceId ?: return@mapNotNull null
            device to deviceId
        }

        // Remove unlinked
        localDevices.filterNot { (_, deviceId) -> deviceId in cloudBarsId }.forEach { (device, _) ->
            removeCloud(device)
        }
    }

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