package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.api.getMapped
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.AutoUpdateChanged
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.AvailableVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion.ReadyToUpdate.*
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AutoUpdate
import net.flipper.bridge.connection.feature.rpc.api.model.GetUpdateChangelogResponse
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.hasCapability
import net.flipper.bsb.cloud.rest.api.BusyFirmwareDirectoryApi
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.orElse
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Suppress("UnusedPrivateProperty", "LongParameterList")
class FFirmwareUpdateFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi,
    private val deviceApi: FConnectedDeviceApi,
    private val lanUpdateVersionProvider: LanUpdateVersionProvider,
) : FFirmwareUpdateFeatureApi, LogTagProvider {
    override val TAG: String = "FFirmwareUpdateFeatureApi"
    private val combinedFlows = UpdateFlowCombinerDelegate(
        rpcFeatureApi = rpcFeatureApi,
        fEventsFeatureApi = fEventsFeatureApi,
        scope = scope
    )

    private val availableVersionFlow = combinedFlows.availableVersionFlow
    override val updateStatusFlow = combinedFlows.updateStatusFlow.wrap()

    override suspend fun setAutoUpdate(isEnabled: Boolean): CResult<Unit> {
        val request = AutoUpdate(
            isEnabled = isEnabled,
            intervalStart = DEFAULT_AUTO_UPDATE_INTERVAL_START,
            intervalEnd = DEFAULT_AUTO_UPDATE_INTERVAL_END
        )
        return rpcFeatureApi.fRpcUpdaterApi.setAutoUpdate(request)
            .map { }
            .onSuccess {
                val event = AutoUpdateChanged(isEnabled)
                fEventsFeatureApi.onBusyLibEvent(event)
            }
            .toCResult()
    }

    override val isAutoUpdateEnabledFlow = fEventsFeatureApi
        .getMapped<AutoUpdateChanged, Boolean>(scope = scope, initial = { couldConsume ->
            rpcFeatureApi
                .fRpcUpdaterApi
                .getAutoUpdate(couldConsume)
                .map(AutoUpdate::isEnabled)
        }, mapper = { it.isEnabled })
        .wrap()

    override val updateVersionFlow = deviceApi
        .tryCast<FHTTPDeviceApi>()
        ?.hasCapability(FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED)
        .orElse { false }
        .distinctUntilChanged()
        .flatMapLatest { useRestApiVersion ->
            info { "#updateVersionFlow useRestApiVersion: $useRestApiVersion" }
            if (useRestApiVersion) {
                lanUpdateVersionProvider.get()
            } else {
                availableVersionFlow
                    .map { version ->
                        when (version) {
                            is AvailableVersion.Available -> Default(
                                version.version
                            )

                            AvailableVersion.NotAvailable -> BsbUpdateVersion.NoUpdateAvailable
                            AvailableVersion.Loading -> BsbUpdateVersion.Loading
                            AvailableVersion.FailedToCheck -> BsbUpdateVersion.FailedToCheck
                            AvailableVersion.CheckingOnBBInProgress -> {
                                BsbUpdateVersion.CheckingOnBBInProgress
                            }
                        }
                    }
            }
        }
        .onEach { info { "#updateVersionFlow: $it" } }
        .stateIn(scope, SharingStarted.Lazily, BsbUpdateVersion.Loading)
        .wrap()

    override val updateVersionChangelog: WrappedFlow<String?> = updateVersionFlow
        .mapLatest { busyBarVersion ->
            return@mapLatest when (busyBarVersion) {
                is Default -> {
                    exponentialRetry {
                        rpcFeatureApi.fRpcUpdaterApi
                            .getUpdateChangelog(busyBarVersion.version)
                            .map(GetUpdateChangelogResponse::changelog)
                    }
                }

                is Url -> {
                    busyBarVersion.changelog
                }

                BsbUpdateVersion.Loading,
                BsbUpdateVersion.CheckingOnBBInProgress,
                BsbUpdateVersion.FailedToCheck,
                BsbUpdateVersion.NoUpdateAvailable -> null
            }
        }
        .onEach { info { "#updateVersionChangelog: $it" } }
        .shareIn(scope, SharingStarted.Lazily, 1)
        .asFlow()
        .wrap()

    @Inject
    class FDeviceFeatureApiFactory(
        private val busyFirmwareDirectoryApi: BusyFirmwareDirectoryApi,
        private val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    ) : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val rpcApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            val fDeviceInfoFeatureApi = unsafeFeatureDeviceApi
                .get(FDeviceInfoFeatureApi::class)
                ?.await()
                ?: return null
            val fEventsFeatureApi = unsafeFeatureDeviceApi
                .get(FEventsFeatureApi::class)
                ?.await() ?: return null
            return FFirmwareUpdateFeatureApiImpl(
                rpcFeatureApi = rpcApi,
                fEventsFeatureApi = fEventsFeatureApi,
                scope = scope,
                deviceApi = connectedDevice,
                lanUpdateVersionProvider = LanUpdateVersionProvider(
                    busyFirmwareDirectoryApi = busyFirmwareDirectoryApi,
                    busyFirmwareDirectoryChannelApi = busyFirmwareDirectoryChannelApi,
                    fDeviceInfoFeatureApi = fDeviceInfoFeatureApi
                )
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface FFeatureComponent {
        @Provides
        @IntoMap
        fun provideFeatureFactory(
            fDeviceFeatureApiFactory: FDeviceFeatureApiFactory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.FIRMWARE_UPDATE to fDeviceFeatureApiFactory
        }
    }
}

private const val DEFAULT_AUTO_UPDATE_INTERVAL_START = "00:00"
private const val DEFAULT_AUTO_UPDATE_INTERVAL_END = "08:00"
