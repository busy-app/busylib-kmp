package net.flipper.bridge.lanmonitor.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.lanmonitor.api.LanMonitorApi
import net.flipper.bridge.lanmonitor.impl.platform.LanAvailablePlatformListener
import net.flipper.bridge.lanmonitor.impl.utils.DeviceMetaInfoRequester
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.eventbus.internal.BusyLibEventPublisher
import net.flipper.eventbus.model.BusyLibEvent

private const val DEFAULT_NAME = "BUSY Bar via LAN"

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<LanMonitorApi>())
@ContributesIntoSet(BusyLibGraph::class, binding = binding<InternalBUSYLibStartupListener>())
class LanMonitorApiImpl(
    lanAvailableListener: LanAvailablePlatformListener,
    globalScope: CoroutineScope,
    private val infoRequester: DeviceMetaInfoRequester,
    private val storageApi: FInternalDevicePersistedStorage,
    private val eventApi: BusyLibEventPublisher
) : LanMonitorApi, InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "LanMonitorApi"
    private val connectedDeviceFlow = lanAvailableListener
        .getLanAvailableFlow()
        .mapLatest { isAvailable ->
            if (isAvailable) {
                info { "Detect plug in for new device" }
                exponentialRetry {
                    infoRequester.getMetaInfo()
                        .onFailure {
                            error(it) { "Failed to request hardware id" }
                        }
                }
            } else {
                null
            }
        }.onEach {
            info { "New device plug in with hardware id: $it" }
        }.stateIn(globalScope, SharingStarted.Eagerly, null)

    override fun getConnectedDeviceFlow() = connectedDeviceFlow

    private val storageUpdaterScope = globalScope.asSingleJobScope()

    override fun onLaunch() {
        storageUpdaterScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            connectedDeviceFlow.collect { connectedDevice ->
                if (connectedDevice != null) {
                    val event = storageApi.transactionInternal {
                        onDevicePlugIn(connectedDevice.hardwareId)
                    }
                    // Publish AFTER the transaction completes. The event bus is a
                    // rendezvous flow, so publish suspends until a subscriber receives
                    // the event. Emitting inside transactionInternal would hold the
                    // global storage lock for that whole time and deadlock if the
                    // subscriber reacts by opening its own storage transaction.
                    if (event != null) {
                        eventApi.publish(event)
                    }
                }
            }
        }
    }

    /**
     * Reconciles storage with a device that just became reachable over LAN and returns the
     * [BusyLibEvent] that must be published once the transaction is committed, or `null` when
     * the active device did not change.
     */
    private fun InternalStorageTransactionScope.onDevicePlugIn(hardwareId: String): BusyLibEvent? {
        val existedDevice = getAllDevices().find { it.hardwareId == hardwareId }
        if (existedDevice != null) {
            info { "Found existed device with hardware id $hardwareId, switch to them" }
            // Auto switch
            val updatedDevice = existedDevice.addTransport(lan = BUSYBar.ConnectionWay.Lan)
            addOrReplace(updatedDevice)
            val wasAlreadyCurrent = existedDevice.uniqueId == getCurrentDevice()?.uniqueId
            setCurrentDevice(updatedDevice)
            return if (wasAlreadyCurrent) {
                null
            } else {
                BusyLibEvent.ActiveDeviceAutoSwitched(updatedDevice)
            }
        }
        val newDevice = BUSYBar(
            humanReadableName = DEFAULT_NAME,
            hardwareId = hardwareId,
            lan = BUSYBar.ConnectionWay.Lan
        )
        info { "Add new device with hardware id: $newDevice" }
        setCurrentDevice(newDevice)
        return BusyLibEvent.ActiveDeviceAutoSwitched(newDevice)
    }
}
