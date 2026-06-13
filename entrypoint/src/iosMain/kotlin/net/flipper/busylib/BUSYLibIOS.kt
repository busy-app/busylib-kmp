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
import net.flipper.bridge.connection.transport.ble.impl.ios.central.FCentralManagerApi
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.Provider
import net.flipper.busylib.di.BUSYLibGraphIOS
import net.flipper.eventbus.api.EventBusApi
import net.flipper.tools.multistream.api.MultiStreamApi

@Inject
class BUSYLibIOS(
    override val connectionService: FConnectionService,
    override val orchestrator: FDeviceOrchestrator,
    override val featureProvider: FFeatureProvider,
    override val firmwareUpdaterApi: FirmwareUpdaterApi,
    override val persistedStorage: FDevicePersistedStorage,
    override val multiStreamApi: MultiStreamApi,
    val fCentralManagerApi: Provider<FCentralManagerApi>,
    private val startUpListeners: Set<InternalBUSYLibStartupListener>,
    override val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    override val eventBus: EventBusApi
) : BUSYLibApple {
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
            observableSettings: ObservableSettings,
            hostApi: BUSYLibHostApi,
            networkStateApi: BUSYLibNetworkStateApi
        ): BUSYLibIOS {
            val graph = createGraphFactory<BUSYLibGraphIOS.Factory>().create(
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

fun Provider<FCentralManagerApi>.get(): FCentralManagerApi {
    return this.invoke()
}
