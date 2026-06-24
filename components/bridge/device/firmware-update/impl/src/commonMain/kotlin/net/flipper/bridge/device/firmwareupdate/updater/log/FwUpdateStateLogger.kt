package net.flipper.bridge.device.firmwareupdate.updater.log

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.verbose

internal class FwUpdateStateLogger : LogTagProvider by TaggedLogger("UpdaterApi") {
    private var lastSignature: List<Any?>? = null

    fun logIfChanged(
        result: FwUpdateState,
        updateStatusSource: UpdateStatusSource,
        bsbUpdateVersion: BsbUpdateVersion?,
        downloaderState: FirmwareDownloaderState,
        uploaderState: FirmwareUploaderState,
        isInstallRequested: Boolean
    ) {
        val signature = listOf(
            updateStatusSource,
            bsbUpdateVersion,
            if (downloaderState is FirmwareDownloaderState.Downloading) {
                FirmwareDownloaderState.Downloading::class
            } else {
                downloaderState
            },
            if (uploaderState is FirmwareUploaderState.Uploading) {
                FirmwareUploaderState.Uploading::class
            } else {
                uploaderState
            }
        )
        if (signature == lastSignature) return
        lastSignature = signature
        verbose {
            "Result is: $result. " +
                "Receive updateStatusSource: $updateStatusSource, " +
                "bsbUpdateVersion: $bsbUpdateVersion, " +
                "downloaderState: $downloaderState, " +
                "uploaderState: $uploaderState," +
                "isInstallRequested: $isInstallRequested"
        }
    }
}
