package net.flipper.busylib

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import com.flipperdevices.core.network.BUSYLibNetworkStateApiNoop
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.api.BUSYLibHostApiStub
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.di.BUSYLibGraphDesktop
import net.flipper.busylib.di.create
import kotlin.collections.forEach

@Inject
class BUSYLibDesktop(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider,
    override val firmwareUpdaterApi: FirmwareUpdaterApi,
    private val startUpListeners: Set<InternalBUSYLibStartupListener>
) : BUSYLib {
    override fun launch() {
        startUpListeners.forEach {
            it.onLaunch()
        }
    }

    companion object {
        @Suppress("LongParameterList", "ForbiddenComment") // TODO: Move it to builder
        fun build(
            scope: CoroutineScope,
            principalApi: BUSYLibPrincipalApi,
            busyLibBarsApi: BUSYLibBarsApi,
            persistedStorage: FDevicePersistedStorage,
            hostApi: BUSYLibHostApi = BUSYLibHostApiStub("cloud.busy.app"),
            networkStateApi: BUSYLibNetworkStateApi = BUSYLibNetworkStateApiNoop()
        ): BUSYLibDesktop {
            val graph = BUSYLibGraphDesktop::class.create(
                scope,
                principalApi,
                busyLibBarsApi,
                persistedStorage,
                hostApi,
                networkStateApi,
                PreferencesSettings.Factory().create("busylib_settings")
            )
            return graph.busyLib
        }
    }
}
