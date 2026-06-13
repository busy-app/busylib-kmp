package net.flipper.bridge.lanmonitor.impl.platform

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.info
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Desktop (JVM) [LanAvailablePlatformListener].
 *
 * Unlike the macOS implementation, which is driven by the native Network
 * framework, this listener uses pure JVM sockets via [LanReachabilityProbe] and
 * polls the device on a fixed [POLL_INTERVAL], publishing each result into a
 * [StateFlow].
 */
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding<LanAvailablePlatformListener>())
class LanAvailableDesktopListener(
    globalScope: CoroutineScope,
    private val probe: LanReachabilityProbe,
) : LanAvailablePlatformListener, LogTagProvider {
    override val TAG: String = "FDesktopLanConnectionMonitor"

    private val lanAvailableStateFlow = MutableStateFlow(false)

    override fun getLanAvailableFlow(): StateFlow<Boolean> = lanAvailableStateFlow

    init {
        info { "#init Started LAN availability monitoring" }
        globalScope.launch {
            while (isActive) {
                val reachable = probe.isReachable()
                debug { "#monitor reachable=$reachable" }
                lanAvailableStateFlow.emit(reachable)
                delay(POLL_INTERVAL)
            }
        }
    }

    internal companion object {
        internal val POLL_INTERVAL: Duration = 2.seconds
    }
}
