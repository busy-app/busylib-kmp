package net.flipper.bridge.lanmonitor.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.bridge.lanmonitor.api.LanMonitorApi
import net.flipper.bridge.lanmonitor.impl.platform.LanAvailablePlatformListener
import net.flipper.bridge.lanmonitor.impl.utils.DeviceMetaInfoRequester
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, LanMonitorApi::class)
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class LanMonitorApiImpl(
    lanAvailableListener: LanAvailablePlatformListener,
    globalScope: CoroutineScope,
    private val infoRequester: DeviceMetaInfoRequester
) : LanMonitorApi, InternalBUSYLibStartupListener {
    private val connectedDeviceFlow = lanAvailableListener
        .getLanAvailableFlow()
        .mapLatest { isAvailable ->
            if (isAvailable) {
                exponentialRetry {
                    infoRequester.getMetaInfo()
                }
            } else null
        }.stateIn(globalScope, SharingStarted.Eagerly, null)

    override fun getConnectedDeviceFlow() = connectedDeviceFlow

    private val storageUpdaterScope = globalScope.asSingleJobScope()

    override fun onLaunch() {
        storageUpdaterScope.launch(SingleJobMode.CANCEL_PREVIOUS) {

        }
    }
}
