package net.flipper.bridge.device.firmwareupdate.uploader.api

import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.onLatest
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.util.asFlow
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, FirmwareUploaderApi::class)
class FirmwareUploaderApiImpl(
    private val fFeatureProvider: FFeatureProvider,
    private val fDeviceOrchestrator: FDeviceOrchestrator
) : FirmwareUploaderApi, LogTagProvider by TaggedLogger("FirmwareUploaderApi") {
    private val _state = MutableStateFlow<FirmwareUploaderState>(FirmwareUploaderState.Pending)
    override val state: StateFlow<FirmwareUploaderState> = _state.asStateFlow()

    private suspend fun awaitDeviceDisconnected() {
        fDeviceOrchestrator.getState()
            .onEach { info { "#awaitDeviceDisconnected: $it" } }
            .filterIsInstance<FDeviceConnectStatus.Disconnected>()
            .first()
    }

    private suspend fun awaitDeviceConnected() {
        fDeviceOrchestrator.getState()
            .onEach { info { "#awaitDeviceConnected: $it" } }
            .filterIsInstance<FDeviceConnectStatus.Connected>()
            .first()
    }

    override suspend fun uploadAndInstall(clientFilePath: Path): Result<Unit> {
        fFeatureProvider.get<FRpcFeatureApi>()
            .onEach { _state.emit(FirmwareUploaderState.Pending) }
            .map { fFeatureStatus -> fFeatureStatus.tryCast<FFeatureStatus.Supported<FRpcFeatureApi>>() }
            .map { status -> status?.featureApi?.fRpcUpdaterApi }
            .filterNotNull()
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
                            _state.update { FirmwareUploaderState.Uploading(bytesUploaded, size) }
                        }
                    ).getOrThrow()
                } catch (_: SocketTimeoutException) {
                    info { "#uploadAndInstall device connection lost" }
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
        info { "#uploadAndInstall upload finished!" }
        awaitDeviceDisconnected()
        info { "#uploadAndInstall device disconnected" }
        awaitDeviceConnected()
        info { "#uploadAndInstall device connected" }
        _state.emit(FirmwareUploaderState.Pending)
        return Result.success(Unit)
    }
}
