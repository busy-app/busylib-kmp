package net.flipper.busylib

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.busylib.di.create

@Inject
class BUSYLibMacOS(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider
) : BUSYLibApple {
    companion object {
        fun build(
            scope: CoroutineScope,
            principalApi: BUSYLibPrincipalApi,
            busyLibBarsApi: BUSYLibBarsApi,
            persistedStorage: FDevicePersistedStorage,
        ): BUSYLibMacOS {
            val graph = create(
                scope,
                principalApi,
                busyLibBarsApi,
                persistedStorage
            )
            return graph.busyLib
        }
    }
}
