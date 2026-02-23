package net.flipper.bridge.device.firmwareupdate.uploader.model

sealed interface FirmwareUploaderState {
    data object Pending : FirmwareUploaderState
    data object Uploaded : FirmwareUploaderState
    data class Uploading(
        val bytesReceived: Long,
        val totalBytes: Long
    ) : FirmwareUploaderState {
        val progress: Float = when {
            totalBytes <= 0 -> 0f
            else -> bytesReceived.toFloat()
                .div(totalBytes)
                .coerceIn(0f, 1f)
        }
    }
}
