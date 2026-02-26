package net.flipper.bridge.device.firmwareupdate.downloader.api

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.downloader.util.sha256
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.util.asFlow

internal class FirmwareDownloaderApiImpl(
    private val httpClient: HttpClient,
) : FirmwareDownloaderApi, LogTagProvider by TaggedLogger("FirmwareDownloaderApi") {
    private val _state = MutableStateFlow<FirmwareDownloaderState>(FirmwareDownloaderState.Pending)
    override val state: StateFlow<FirmwareDownloaderState> = _state.asStateFlow()

    private fun getTemporalPath(): Path {
        return Path(SystemTemporaryDirectory, "temp_firmware_update_file")
    }

    private suspend fun downloadIntoFile(
        sink: RawSink,
        bytesFlow: Flow<ByteArray>,
        totalBytes: Long
    ) {
        val buffer = Buffer()
        var downloadedBytes = 0L
        bytesFlow
            .onCompletion { _state.emit(FirmwareDownloaderState.Downloaded) }
            .collect { chunk ->
                downloadedBytes += chunk.size
                buffer.clear()
                buffer.write(chunk)
                sink.write(buffer, buffer.size)
                _state.emit(
                    value = FirmwareDownloaderState.Downloading(
                        bytesReceived = downloadedBytes,
                        totalBytes = totalBytes
                    )
                )
            }
        buffer.close()
    }

    override fun reset() {
        _state.update { FirmwareDownloaderState.Pending }
    }

    override suspend fun download(bsbUpdateVersion: BsbUpdateVersion.Url): Result<Path> {
        _state.emit(FirmwareDownloaderState.Pending)
        return runSuspendCatching {
            _state.emit(
                value = FirmwareDownloaderState.Downloading(
                    bytesReceived = 0L,
                    totalBytes = 0L
                )
            )

            val temporalFilePath = getTemporalPath()
            httpClient.prepareGet(bsbUpdateVersion.url).execute { response ->
                val totalBytes = response.contentLength() ?: 0L
                if (totalBytes == 0L) {
                    error { "#downloadAndUpload size cannot be 0" }
                    return@execute
                }

                _state.emit(
                    value = FirmwareDownloaderState.Downloading(
                        bytesReceived = 0L,
                        totalBytes = totalBytes
                    )
                )
                SystemFileSystem.sink(path = temporalFilePath, append = false).use { sink ->
                    downloadIntoFile(
                        sink = sink,
                        bytesFlow = response.bodyAsChannel().asFlow(),
                        totalBytes = totalBytes
                    )
                }
            }

            info { "#downloadAndUpload download finished!" }
            if (temporalFilePath.sha256() != bsbUpdateVersion.sha256) {
                error("Downloaded file hash does not match expected hash")
            }
            _state.emit(FirmwareDownloaderState.Downloaded)
            temporalFilePath
        }.onFailure { t ->
            error(t) { "#downloadAndUpload could not finish download" }
            _state.emit(FirmwareDownloaderState.Pending)
        }
    }
}
