package net.flipper.busylib

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bsb.auth.principal.api.BsbUserPrincipalApi
import net.flipper.bsb.cloud.api.BSBBarsApi
import net.flipper.busylib.di.BUSYLibGraphDesktop
import net.flipper.busylib.di.create

@Inject
class BUSYLibDesktop(
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
        ): BUSYLibDesktop {
            val graph = BUSYLibGraphDesktop::class.create(
                scope,
                principalApi,
                bsbBarsApi,
                persistedStorage
            )
            return graph.busyLib
        }
    }
}
