package net.flipper.bridge.device.firmwareupdate.uploader.api

import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState
import net.flipper.core.busylib.ktx.common.onLatest
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.util.asFlow

internal class FirmwareUploaderApiImpl(
    private val fFeatureProvider: FFeatureProvider,
) : FirmwareUploaderApi, LogTagProvider by TaggedLogger("FirmwareUploaderApi") {
    private val _state = MutableStateFlow<FirmwareUploaderState>(FirmwareUploaderState.Pending)
    override val state: StateFlow<FirmwareUploaderState> = _state.asStateFlow()

    override fun reset() {
        _state.update { FirmwareUploaderState.Pending }
    }

    override suspend fun uploadAndInstall(
        clientFilePath: Path,
        onPrepared: suspend () -> Unit
    ): Result<Unit> {
        _state.emit(FirmwareUploaderState.Uploading(0, 0))
        onPrepared.invoke()
        fFeatureProvider.get<FRpcFeatureApi>()
            .map { fFeatureStatus -> fFeatureStatus.tryCast<FFeatureStatus.Supported<FRpcFeatureApi>>() }
            .map { status -> status?.featureApi?.fRpcUpdaterApi }
            .filterNotNull()
            .onEach { _state.emit(FirmwareUploaderState.Pending) }
            .onLatest { fFeatureApi ->
                _state.emit(FirmwareUploaderState.Uploading(0, 0))
                val size = SystemFileSystem.metadataOrNull(clientFilePath)?.size ?: 0L
                if (size == 0L) {
                    error("#uploadAndInstall: could not read file size")
                }
                _state.emit(FirmwareUploaderState.Uploading(0, size))
                try {
                    fFeatureApi.postUpdate(
                        totalBytes = size,
                        bytesFlow = SystemFileSystem.source(clientFilePath).asFlow(),
                        onTransferred = { bytesUploaded ->
                            val state = FirmwareUploaderState.Uploading(bytesUploaded, size)
                            if (state.progress >= 1f) {
                                _state.update { FirmwareUploaderState.Uploaded }
                            } else {
                                _state.update { state }
                            }
                        }
                    ).getOrThrow()
                } catch (_: SocketTimeoutException) {
                    info { "#uploadAndInstall device connection lost" }
                    _state.emit(FirmwareUploaderState.Uploaded)
                }
            }
            .catch { t ->
                // don't reset state without throwable!
                error(t) { "#uploadAndInstall could not post update" }
                _state.emit(FirmwareUploaderState.Failed)
            }
            .first()
        if (_state.first() is FirmwareUploaderState.Failed) {
            return Result.failure(Exception("Upload failed"))
        }
        _state.emit(FirmwareUploaderState.Uploaded)
        return Result.success(Unit)
    }
}
