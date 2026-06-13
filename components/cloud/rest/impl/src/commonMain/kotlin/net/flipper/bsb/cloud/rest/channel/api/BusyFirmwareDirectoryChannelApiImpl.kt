package net.flipper.bsb.cloud.rest.channel.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bsb.cloud.rest.channel.krate.BsbFirmwareChannelIdKrate
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId
import net.flipper.busylib.core.di.BusyLibGraph

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<BusyFirmwareDirectoryChannelApi>())
class BusyFirmwareDirectoryChannelApiImpl(
    private val bsbFirmwareChannelIdKrate: BsbFirmwareChannelIdKrate
) : BusyFirmwareDirectoryChannelApi {
    override fun getChannelIdFlow(): StateFlow<BsbFirmwareChannelId> {
        return bsbFirmwareChannelIdKrate.cachedStateFlow
    }

    override suspend fun setChannel(channel: BsbFirmwareChannelId) {
        bsbFirmwareChannelIdKrate.save(channel)
    }
}
