package net.flipper.bridge.connection.orchestrator.api

import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FDeviceOrchestrator {

    fun getState(): WrappedStateFlow<FDeviceConnectStatus>

    suspend fun connectIfNot(config: FDeviceBaseModel)

    suspend fun disconnectCurrent()
}
