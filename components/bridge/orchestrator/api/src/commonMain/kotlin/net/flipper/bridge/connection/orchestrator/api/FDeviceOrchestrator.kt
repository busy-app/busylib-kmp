package net.flipper.bridge.connection.orchestrator.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus

interface FDeviceOrchestrator {

    fun getState(): StateFlow<FDeviceConnectStatus>

    suspend fun connect(config: FDeviceBaseModel)

    suspend fun disconnectCurrent()
}
