package net.flipper.bridge.lanmonitor.impl.platform

import me.tatarka.inject.annotations.Provides
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(BusyLibGraph::class)
interface DesktopLanMonitorModule {

    @Provides
    @SingleIn(BusyLibGraph::class)
    fun provideLanReachabilityProbe(): LanReachabilityProbe {
        return TcpSocketLanReachabilityProbe()
    }
}
