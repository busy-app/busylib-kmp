package net.flipper.bridge.device.firmwareupdate.downloader.api

import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.files.Path
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState

internal interface FirmwareDownloaderApi {
    val state: StateFlow<FirmwareDownloaderState>
    suspend fun download(bsbUpdateVersion: BsbUpdateVersion.Url): Result<Path>
    fun reset()
}
