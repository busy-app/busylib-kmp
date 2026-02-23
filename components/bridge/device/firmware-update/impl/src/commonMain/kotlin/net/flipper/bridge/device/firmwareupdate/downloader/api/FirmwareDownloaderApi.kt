package net.flipper.bridge.device.firmwareupdate.downloader.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState

interface FirmwareDownloaderApi {
    val state: StateFlow<FirmwareDownloaderState>
    suspend fun downloadAndUpload(version: String)
}
