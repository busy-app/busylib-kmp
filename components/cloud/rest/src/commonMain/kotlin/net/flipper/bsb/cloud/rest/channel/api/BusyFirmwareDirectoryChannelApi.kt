package net.flipper.bsb.cloud.rest.channel.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId

interface BusyFirmwareDirectoryChannelApi {
    suspend fun getChannelIdFlow(): StateFlow<BsbFirmwareChannelId>
}
