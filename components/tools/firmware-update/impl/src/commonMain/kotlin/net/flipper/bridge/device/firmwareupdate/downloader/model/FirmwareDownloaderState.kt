package net.flipper.bridge.device.firmwareupdate.downloader.model

internal sealed interface FirmwareDownloaderState {
    data object Downloaded : FirmwareDownloaderState
    data object Pending : FirmwareDownloaderState
    data class Downloading(
        val bytesReceived: Long,
        val totalBytes: Long
    ) : FirmwareDownloaderState {
        val progress: Float = when {
            totalBytes <= 0 -> 0f
            else -> bytesReceived.toFloat()
                .div(totalBytes)
                .coerceIn(0f, 1f)
        }
    }
}
