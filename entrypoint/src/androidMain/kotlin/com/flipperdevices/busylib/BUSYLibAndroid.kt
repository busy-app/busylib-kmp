package com.flipperdevices.busylib

import android.content.Context
import com.flipperdevices.bridge.api.scanner.FlipperScanner
import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.feature.provider.api.FFeatureProvider
import com.flipperdevices.bridge.connection.orchestrator.api.FDeviceOrchestrator
import com.flipperdevices.bridge.connection.service.api.FConnectionService
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import com.flipperdevices.busylib.di.BUSYLibGraphAndroid
import me.tatarka.inject.annotations.Inject
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope

@Inject
class BUSYLibAndroid(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider,
    val flipperScanner: FlipperScanner
) : BUSYLib {
    companion object {
        fun build(
            scope: CoroutineScope,
            principalApi: BsbUserPrincipalApi,
            bsbBarsApi: BSBBarsApi,
            persistedStorage: FDevicePersistedStorage,
            // Android-specific factory
            context: Context,
        ): BUSYLibAndroid {
            val graph = createGraphFactory<BUSYLibGraphAndroid.Factory>()
                .create(
                    scope,
                    principalApi,
                    bsbBarsApi,
                    persistedStorage,
                    context
                )
            return graph.busyLib
        }
    }
}