package com.flipperdevices.busylib

import com.flipperdevices.bridge.connection.feature.provider.api.FFeatureProvider
import com.flipperdevices.bridge.connection.orchestrator.api.FDeviceOrchestrator
import com.flipperdevices.bridge.connection.service.api.FConnectionService

interface BUSYLib {
    val connectionService: FConnectionService
    val orchestrator: FDeviceOrchestrator
    val featureProvider: FFeatureProvider
}