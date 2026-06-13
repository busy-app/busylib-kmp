package net.flipper.busylib

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.di.BUSYLibGraphMacOS
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.eventbus.api.EventBusApi
import net.flipper.tools.multistream.api.MultiStreamApi
import net.flipper.tools.oncall.api.OnCallSingletonApi

@Inject
class BUSYLibMacOS(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider,
    override val firmwareUpdaterApi: FirmwareUpdaterApi,
    override val persistedStorage: FDevicePersistedStorage,
    override val multiStreamApi: MultiStreamApi,
    private val startUpListeners: Set<InternalBUSYLibStartupListener>,
    val onCallSingletonApi: OnCallSingletonApi,
    override val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    override val eventBus: EventBusApi
) : BUSYLibApple, LogTagProvider {
    override val TAG = "BUSYLib"
    override fun launch() {
        info { "Start BUSY Lib local version" }
        startUpListeners.forEach {
            it.onLaunch()
        }
    }

    companion object {
        @Suppress("LongParameterList", "ForbiddenComment") // TODO: Move it to builder
        fun build(
            scope: CoroutineScope,
            principalApi: BUSYLibPrincipalApi,
            observableSettings: ObservableSettings,
            hostApi: BUSYLibHostApi,
            networkStateApi: BUSYLibNetworkStateApi
        ): BUSYLibMacOS {
            val graph = createGraphFactory<BUSYLibGraphMacOS.Factory>().create(
                scope,
                principalApi,
                observableSettings,
                hostApi,
                networkStateApi,
                NSUserDefaultsSettings.Factory().create("busylib_settings")
            )
            return graph.busyLib
        }
    }
}
