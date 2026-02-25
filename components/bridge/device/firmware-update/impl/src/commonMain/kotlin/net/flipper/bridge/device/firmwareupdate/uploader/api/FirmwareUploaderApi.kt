package net.flipper.bridge.device.firmwareupdate.uploader.api

import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.files.Path
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState

interface FirmwareUploaderApi {
    val state: StateFlow<FirmwareUploaderState>
    suspend fun uploadAndInstall(clientFilePath: Path): Result<Unit>
}
