package net.flipper.busylib

import net.flipper.bridge.connection.feature.firmwareupdate.updater.api.UpdaterApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService

interface BUSYLib {
    val connectionService: FConnectionService
    val orchestrator: FDeviceOrchestrator
    val featureProvider: FFeatureProvider
    val updaterApi: UpdaterApi
}
