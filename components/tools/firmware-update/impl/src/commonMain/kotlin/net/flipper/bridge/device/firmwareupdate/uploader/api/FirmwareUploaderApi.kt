package net.flipper.bridge.device.firmwareupdate.uploader.api

import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.files.Path
import net.flipper.bridge.device.firmwareupdate.updater.model.StartUpdateResponse
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState

internal interface FirmwareUploaderApi {
    val state: StateFlow<FirmwareUploaderState>
    suspend fun uploadAndInstall(
        clientFilePath: Path,
        onPrepared: suspend () -> Unit
    ): StartUpdateResponse

    fun reset()
}
