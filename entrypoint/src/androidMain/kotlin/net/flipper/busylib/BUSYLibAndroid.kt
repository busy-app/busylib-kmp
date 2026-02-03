package net.flipper.busylib

import android.content.Context
import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import com.flipperdevices.core.network.BUSYLibNetworkStateApiNoop
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.api.scanner.FlipperScanner
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.firmwareupdate.updater.api.UpdaterApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.api.BUSYLibHostApiStub
import net.flipper.busylib.di.BUSYLibGraphAndroid
import net.flipper.busylib.di.create
import no.nordicsemi.kotlin.ble.client.android.CentralManager

@Inject
class BUSYLibAndroid(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider,
    override val updaterApi: UpdaterApi,
    val flipperScanner: FlipperScanner,
    val centralManager: CentralManager
) : BUSYLib {
    companion object {
        @Suppress("LongParameterList", "ForbiddenComment") // TODO: Move it to builder
        fun build(
            scope: CoroutineScope,
            principalApi: BUSYLibPrincipalApi,
            busyLibBarsApi: BUSYLibBarsApi,
            persistedStorage: FDevicePersistedStorage,
            // Android-specific factory
            context: Context,
            hostApi: BUSYLibHostApi = BUSYLibHostApiStub("cloud.busy.app"),
            networkStateApi: BUSYLibNetworkStateApi = BUSYLibNetworkStateApiNoop()
        ): BUSYLibAndroid {
            val graph = BUSYLibGraphAndroid::class.create(
                scope,
                principalApi,
                busyLibBarsApi,
                persistedStorage,
                context,
                hostApi,
                networkStateApi
            )
            return graph.busyLib
        }
    }
}
