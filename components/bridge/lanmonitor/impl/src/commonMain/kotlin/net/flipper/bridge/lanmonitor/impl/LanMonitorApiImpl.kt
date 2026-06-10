package net.flipper.bridge.lanmonitor.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.bridge.lanmonitor.api.LanMonitorApi
import net.flipper.bridge.lanmonitor.impl.platform.LanAvailablePlatformListener
import net.flipper.bridge.lanmonitor.impl.utils.DeviceMetaInfoRequester
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

private const val DEFAULT_NAME = "BUSY Bar via LAN"

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, LanMonitorApi::class)
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class LanMonitorApiImpl(
    lanAvailableListener: LanAvailablePlatformListener,
    globalScope: CoroutineScope,
    private val infoRequester: DeviceMetaInfoRequester,
    private val storageApi: FInternalDevicePersistedStorage
) : LanMonitorApi, InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "LanMonitorApi"
    private val connectedDeviceFlow = lanAvailableListener
        .getLanAvailableFlow()
        .mapLatest { isAvailable ->
            if (isAvailable) {
                info { "Detect plugin for new device" }
                exponentialRetry {
                    infoRequester.getMetaInfo()
                        .onFailure {
                            error(it) { "Failed to request hardware id" }
                        }
                }
            } else null
        }.onEach {
            info { "New device plug in with hardware id: $it" }
        }.stateIn(globalScope, SharingStarted.Eagerly, null)

    override fun getConnectedDeviceFlow() = connectedDeviceFlow

    private val storageUpdaterScope = globalScope.asSingleJobScope()

    override fun onLaunch() {
        storageUpdaterScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            connectedDeviceFlow.collect { connectedDevice ->
                if (connectedDevice != null) {
                    storageApi.transactionInternal {
                        onDevicePlugIn(connectedDevice.hardwareId)
                    }
                }
            }
        }
    }

    private fun InternalStorageTransactionScope.onDevicePlugIn(hardwareId: String) {
        val existedDevice = getAllDevices().find { it.hardwareId == hardwareId }
        if (existedDevice != null) {
            info { "Found existed device with hardware id $hardwareId, switch to them" }
            // Auto switch
            addOrReplace(
                existedDevice.addTransport(lan = BUSYBar.ConnectionWay.Lan)
            )
            setCurrentDevice(existedDevice)
            return
        }
        val newDevice = BUSYBar(
            humanReadableName = DEFAULT_NAME,
            hardwareId = hardwareId,
            lan = BUSYBar.ConnectionWay.Lan
        )
        info { "Add new device with hardware id: $newDevice" }
        setCurrentDevice(newDevice)
    }
}
