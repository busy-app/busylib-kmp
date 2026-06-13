package net.flipper.bridge.lanmonitor.impl.platform

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import net.flipper.busylib.core.di.BusyLibGraph

@ContributesTo(BusyLibGraph::class)
@BindingContainer
object DesktopLanMonitorModule {

    @Provides
    @SingleIn(BusyLibGraph::class)
    fun provideLanReachabilityProbe(): LanReachabilityProbe {
        return TcpSocketLanReachabilityProbe()
    }
}
