package net.flipper.bridge.lanmonitor.impl

import me.tatarka.inject.annotations.Inject
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.bridge.lanmonitor.api.LanMonitorApi
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, LanMonitorApi::class)
class LanMonitorApiImpl : LanMonitorApi
