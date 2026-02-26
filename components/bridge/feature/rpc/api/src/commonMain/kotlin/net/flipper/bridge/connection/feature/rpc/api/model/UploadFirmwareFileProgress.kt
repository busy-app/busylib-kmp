package net.flipper.bridge.connection.feature.rpc.api.model

data class UploadFirmwareFileProgress(
    val downloaded: Long,
    val transferred: Long
)
