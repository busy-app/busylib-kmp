package net.flipper.bridge.device.firmwareupdate.uploader.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState
import okio.Path

interface FirmwareUploaderApi {
    val state: StateFlow<FirmwareUploaderState>
    suspend fun uploadAndInstall(clientFilePath: Path)
}
