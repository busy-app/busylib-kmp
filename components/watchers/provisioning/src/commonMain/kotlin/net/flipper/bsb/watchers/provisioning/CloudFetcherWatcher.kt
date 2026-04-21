package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
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
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

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
                val localCloudDeviceIds = allDevices.mapNotNull { it.cloud?.deviceId }
                val cloudBarsId = cloudBars.map { it.id }
                val idsMatch = localCloudDeviceIds.sorted() == cloudBarsId.sorted()
                val hasMismatch = hasCloudMismatch(allDevices, cloudBars)
                if (idsMatch && !hasMismatch) {
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

    private fun hasCloudMismatch(
        allDevices: List<BUSYBar>,
        cloudBars: List<BusyCloudBar>
    ): Boolean {
        val cloudBarsById = cloudBars.associateBy { it.id }
        return allDevices.any { device ->
            val id = device.cloud?.deviceId ?: return@any false
            val bar = cloudBarsById[id] ?: return@any true
            val labelMismatch = bar.label != null && bar.label != device.humanReadableName
            val hardwareIdMissing = device.hardwareId == null
            labelMismatch || hardwareIdMissing
        }
    }

    private fun InternalStorageTransactionScope.invalidateCloudBars(
        cloudBars: List<BusyCloudBar>
    ) {
        val cloudBarsById = cloudBars.associateBy { it.id }
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

        // Update still-linked devices: fill missing hardwareId and/or rename when cloud label
        // changed. A null cloud label is treated as "no opinion" and must not overwrite.
        deviceClouds.filter { (_, deviceId) -> deviceId in cloudBarsId }
            .forEach { (device, deviceId) ->
                val bar = cloudBarsById[deviceId] ?: return@forEach
                val updatedName = bar.label ?: device.humanReadableName
                if (updatedName == device.humanReadableName && bar.hardwareId == device.hardwareId) {
                    return@forEach
                }
                info {
                    "Update ${device.uniqueId}: " +
                        "name='${device.humanReadableName}'->'$updatedName', " +
                        "hardwareId='${device.hardwareId}'->'${bar.hardwareId}'"
                }
                addOrReplace(
                    device.copy(
                        humanReadableName = updatedName,
                        hardwareId = bar.hardwareId
                    )
                )
            }

        // Add new link
        cloudBarsById.filterKeys { id -> id !in deviceCloudIds }
            .forEach { (id, bar) ->
                info { "Add new bar: $bar" }
                val newBusyBar = BUSYBar(
                    humanReadableName = bar.label ?: DEFAULT_BUSY_BAR_NAME,
                    cloud = BUSYBar.ConnectionWay.Cloud(deviceId = id),
                    hardwareId = bar.hardwareId
                )
                addOrReplace(newBusyBar)
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
