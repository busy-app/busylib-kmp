package net.flipper.bridge.connection.orchestrator.internal

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator

/**
 * This is internal BUSY Lib class, don't use it outside BUSY Lib
 */
interface FInternalDeviceOrchestrator : FDeviceOrchestrator {
    suspend fun connectIfNot(config: BUSYBar)

    /**
     * See FConnectionService#forget instead
     */
    suspend fun disconnectCurrent()
}
