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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.downloader.util.sha256
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.util.asFlow
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, FirmwareDownloaderApi::class)
class FirmwareDownloaderApiImpl(
    @KtorNetworkClientQualifier private val httpClient: HttpClient,
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
        var downloadedBytes = 0L
        try {
            bytesFlow
                .onEach { chunk ->
                    downloadedBytes += chunk.size
                    _state.emit(
                        value = FirmwareDownloaderState.Downloading(
                            bytesReceived = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    )
                }
                .onEach { chunk ->
                    val buffer = Buffer().apply {
                        write(chunk)
                    }
                    sink.write(buffer, buffer.size)
                }
                .onCompletion { _state.emit(FirmwareDownloaderState.Downloaded) }
                .collect()
        } finally {
            sink.close()
        }
    }

    override fun reset() {
        _state.update { FirmwareDownloaderState.Pending }
    }

    override suspend fun download(bsbUpdateVersion: BsbUpdateVersion.Url): Result<Path> {
        _state.emit(FirmwareDownloaderState.Pending)
        return try {
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
                downloadIntoFile(
                    sink = SystemFileSystem.sink(path = temporalFilePath, append = false),
                    bytesFlow = response.bodyAsChannel().asFlow(),
                    totalBytes = totalBytes
                )
            }

            info { "#downloadAndUpload download finished!" }
            if (temporalFilePath.sha256() != bsbUpdateVersion.sha256) {
                error("Downloaded file hash does not match expected hash")
            }
            _state.emit(FirmwareDownloaderState.Downloaded)
            Result.success(temporalFilePath)
        } catch (t: Throwable) {
            error(t) { "#downloadAndUpload could not finish download" }
            _state.emit(FirmwareDownloaderState.Pending)
            Result.failure(t)
        }
    }
}
