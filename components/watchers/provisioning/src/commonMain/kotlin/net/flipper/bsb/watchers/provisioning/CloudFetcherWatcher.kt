package net.flipper.bsb.watchers.provisioning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.bsb.watchers.provisioning.api.CloudFetcher
import net.flipper.bsb.watchers.provisioning.api.model.CloudProvisioningBar
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.uuid.Uuid

private const val DEFAULT_BUSY_BAR_NAME = "BUSY Bar"

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class CloudFetcherWatcher(
    scope: CoroutineScope,
    private val persistedStorage: FDevicePersistedStorage,
    private val cloudFetcher: CloudFetcher
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "CloudFetcherWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch {
            combine(
                persistedStorage.getAllDevicesFlow(),
                cloudFetcher.getBarsFlow()
            ) { allDevices, cloudBars ->
                allDevices.mapNotNull {
                    it.connectionWays.filterIsInstance<BUSYBar.ConnectionWay.Cloud>().firstOrNull()
                }.map { it.deviceId } to cloudBars
            }.collectLatest { (localCloudDeviceIds, cloudBars) ->
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
        cloudBars: List<CloudProvisioningBar>
    ) {
        val cloudBarsId = cloudBars.mapNotNull { Uuid.parseOrNull(it.id) }.toSet()
        val deviceClouds = getAllDevices().mapNotNull { device ->
            val deviceId = device.connectionWays.filterIsInstance<BUSYBar.ConnectionWay.Cloud>()
                .firstOrNull()?.deviceId ?: return@mapNotNull null
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
                    connectionWays = listOf(
                        BUSYBar.ConnectionWay.Cloud(
                            deviceId = id
                        )
                    )
                )
                addOrReplace(newBusyBar)
            }
    }

    private fun PersistedStorageTransactionScope.removeCloud(
        device: BUSYBar
    ) {
        val newConnectionWays = device.connectionWays.filterNot {
            it is BUSYBar.ConnectionWay.Cloud
        }
        if (newConnectionWays.isEmpty()) {
            info { "Remove $device, because cloud not found in linked device" }
            removeDevice(device.uniqueId)
        } else {
            info { "Remove cloud link from $device, new connections: $newConnectionWays" }
            addOrReplace(device.copy(connectionWays = newConnectionWays))
        }
    }
}

