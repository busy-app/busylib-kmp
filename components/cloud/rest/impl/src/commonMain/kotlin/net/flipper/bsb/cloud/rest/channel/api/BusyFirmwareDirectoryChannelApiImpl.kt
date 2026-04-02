package net.flipper.bsb.cloud.rest.channel.api

import kotlinx.coroutines.flow.StateFlow
import dev.zacsweers.metro.Inject
import net.flipper.bsb.cloud.rest.channel.krate.BsbFirmwareChannelIdKrate
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<BusyFirmwareDirectoryChannelApi>())
class BusyFirmwareDirectoryChannelApiImpl(
    private val bsbFirmwareChannelIdKrate: BsbFirmwareChannelIdKrate
) : BusyFirmwareDirectoryChannelApi {
    override suspend fun getChannelIdFlow(): StateFlow<BsbFirmwareChannelId> {
        return bsbFirmwareChannelIdKrate.cachedStateFlow
    }
}
