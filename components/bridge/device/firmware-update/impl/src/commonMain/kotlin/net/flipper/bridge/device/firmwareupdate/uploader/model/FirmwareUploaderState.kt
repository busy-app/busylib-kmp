package net.flipper.bridge.device.firmwareupdate.uploader.model

internal sealed interface FirmwareUploaderState {
    data object Failed : FirmwareUploaderState
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
