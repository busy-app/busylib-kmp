package net.flipper.busylib

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.api.scanner.FlipperScanner
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.busylib.di.BUSYLibGraphAndroid
import net.flipper.busylib.di.create
import no.nordicsemi.kotlin.ble.client.android.CentralManager

@Inject
class BUSYLibAndroid(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider,
    val flipperScanner: FlipperScanner,
    val centralManager: CentralManager
) : BUSYLib {
    companion object {
        fun build(
            scope: CoroutineScope,
            principalApi: BUSYLibPrincipalApi,
            busyLibBarsApi: BUSYLibBarsApi,
            persistedStorage: FDevicePersistedStorage,
            // Android-specific factory
            context: Context,
        ): BUSYLibAndroid {
            val graph = BUSYLibGraphAndroid::class.create(
                scope,
                principalApi,
                busyLibBarsApi,
                persistedStorage,
                context
            )
            return graph.busyLib
        }
    }
}
