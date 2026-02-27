package net.flipper.bsb.cloud.rest.channel.api

import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.rest.channel.krate.BsbFirmwareChannelIdKrate
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BusyFirmwareDirectoryChannelApi::class)
class BusyFirmwareDirectoryChannelApiImpl(
    private val bsbFirmwareChannelIdKrate: BsbFirmwareChannelIdKrate
) : BusyFirmwareDirectoryChannelApi {
    override suspend fun getChannelIdFlow(): StateFlow<BsbFirmwareChannelId> {
        return bsbFirmwareChannelIdKrate.cachedStateFlow
    }
}
