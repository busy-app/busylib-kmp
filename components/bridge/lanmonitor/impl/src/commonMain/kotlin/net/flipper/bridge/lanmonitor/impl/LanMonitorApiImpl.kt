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
import net.flipper.core.busylib.ktx.common.exponentialRetry
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding


@Inject
@ContributesBinding(BusyLibGraph::class, LanMonitorApi::class)
class LanMonitorApiImpl(
    lanAvailableListener: LanAvailablePlatformListener,
    globalScope: CoroutineScope,
    private val infoRequester: DeviceMetaInfoRequester
) : LanMonitorApi {
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
}
