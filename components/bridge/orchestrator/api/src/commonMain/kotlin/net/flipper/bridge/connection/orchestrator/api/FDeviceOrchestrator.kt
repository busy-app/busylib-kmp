package net.flipper.bridge.connection.orchestrator.api

import kotlinx.coroutines.flow.first
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FDeviceOrchestrator {

    fun getState(): WrappedStateFlow<FDeviceConnectStatus>

    suspend fun connect(config: FDeviceBaseModel)

    suspend fun disconnectCurrent()
}

suspend fun FDeviceOrchestrator.reconnect(): Result<Unit> {
    val latestDevice = when (val state = getState().first()) {
        is FDeviceConnectStatus.Connected -> state.device
        is FDeviceConnectStatus.Connecting -> state.device
        is FDeviceConnectStatus.Disconnected -> state.device
        is FDeviceConnectStatus.Disconnecting -> state.device
    }
    if (latestDevice == null) {
        return Result.failure(IllegalStateException("Latest device was null"))
    }
    disconnectCurrent()
    connect(latestDevice)
    return Result.success(Unit)
}
