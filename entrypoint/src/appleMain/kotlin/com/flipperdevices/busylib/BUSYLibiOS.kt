package com.flipperdevices.busylib

import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.feature.provider.api.FFeatureProvider
import com.flipperdevices.bridge.connection.orchestrator.api.FDeviceOrchestrator
import com.flipperdevices.bridge.connection.service.api.FConnectionService
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.di.BUSYLibGraphIOS
import me.tatarka.inject.annotations.Inject
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope

@Inject
class BUSYLibIOS(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider
) : BUSYLib {
    companion object {
        fun build(
            scope: CoroutineScope,
            principalApi: BsbUserPrincipalApi,
            bsbBarsApi: BSBBarsApi,
            persistedStorage: FDevicePersistedStorage,
        ): BUSYLibIOS {
            val graph = createGraphFactory<BUSYLibGraphIOS.Factory>()
                .create(
                    scope,
                    principalApi,
                    bsbBarsApi,
                    persistedStorage
                )
            return graph.busyLib
        }
    }
}