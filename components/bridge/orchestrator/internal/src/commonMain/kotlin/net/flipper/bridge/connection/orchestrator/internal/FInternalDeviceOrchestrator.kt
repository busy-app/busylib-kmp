package net.flipper.bridge.connection.orchestrator.internal

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator

/**
 * This is internal BUSY Lib class, don't use it outside BUSY Lib
 */
interface FInternalDeviceOrchestrator : FDeviceOrchestrator {
    suspend fun connectIfNot(config: BUSYBar)

    /**
     * Internal helper used by higher-level "forget" operations, such as
     * FConnectionService.forgetDevice(...). This is not exposed on
     * FDeviceOrchestrator's public API and should only be used inside the
     * BUSY Lib orchestration layer.
     */
    suspend fun disconnectCurrent()
}
