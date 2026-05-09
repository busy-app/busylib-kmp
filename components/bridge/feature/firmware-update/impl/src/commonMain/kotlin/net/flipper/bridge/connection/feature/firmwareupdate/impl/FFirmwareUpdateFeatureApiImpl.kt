package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.toBsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.util.merge
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.generated.model.AutoupdateSettings
import net.flipper.bridge.connection.feature.rpc.generated.model.GetUpdateChangelog200Response
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.hasCapability
import net.flipper.bsb.cloud.rest.api.BusyFirmwareDirectoryApi
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orElse
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.throttleLatestCached
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Suppress("UnusedPrivateProperty", "LongParameterList")
class FFirmwareUpdateFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
    private val deviceApi: FConnectedDeviceApi,
    private val lanUpdateVersionProvider: LanUpdateVersionProvider,
) : FFirmwareUpdateFeatureApi, LogTagProvider {
    override val TAG: String = "FFirmwareUpdateFeatureApi"

    override val updateStatusFlow: WrappedSharedFlow<BsbUpdateStatus> = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.Update>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatestCached { event, value: BsbUpdateStatus? ->
                when (event) {
                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.Update> if value != null -> {
                        when (val updateEvent = event.busyLibUpdateEvent) {
                            is BusyLibUpdateEvent.Update.UpdateCheck -> {
                                updateEvent.availableVersion
                                    ?.let { availableVersion ->
                                        value.copy(check = value.check.copy(availableVersion = availableVersion))
                                    }
                                    ?: value
                            }

                            is BusyLibUpdateEvent.Update.UpdateDownload,
                            is BusyLibUpdateEvent.Update.UpdateState -> {
                                value.merge(updateEvent)
                            }
                        }
                    }

                    else -> {
                        exponentialRetry {
                            rpcFeatureApi.fRpcUpdaterApi
                                .getFirmwareUpdateStatus()
                                .map { updateStatus -> updateStatus.toBsbUpdateStatus() }
                                .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
                        }
                    }
                }
            }
        }
        .wrap()

    override suspend fun setAutoUpdate(isEnabled: Boolean): CResult<Unit> {
        val request = AutoupdateSettings(
            isEnabled = isEnabled,
            intervalStart = DEFAULT_AUTO_UPDATE_INTERVAL_START,
            intervalEnd = DEFAULT_AUTO_UPDATE_INTERVAL_END
        )
        return rpcFeatureApi.fRpcUpdaterApi.setAutoupdateSettings(request)
            .map { }
            .onSuccess {
                val event = BusyLibUpdateEvent.AutoUpdateChanged(isEnabled)
                fEventsFeatureApi?.onBusyLibEvent(event)
            }
            .toCResult()
    }

    override val isAutoUpdateEnabledFlow = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.AutoUpdateChanged>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                val couldConsume = consumable.tryConsume()
                when (consumable) {
                    ConsumableUpdateEvent.Empty -> {
                        exponentialRetry {
                            rpcFeatureApi
                                .fRpcUpdaterApi
                                .getAutoupdateSettings()
                                .map(AutoupdateSettings::isEnabled)
                        }
                    }

                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.AutoUpdateChanged> -> {
                        consumable.busyLibUpdateEvent.isEnabled
                    }
                }
            }
        }
        .wrap()

    override val updateVersionFlow: WrappedFlow<BsbUpdateVersion?> = deviceApi
        .tryCast<FHTTPDeviceApi>()
        ?.hasCapability(FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED)
        .orElse { false }
        .distinctUntilChanged()
        .flatMapLatest { useRestApiVersion ->
            info { "#updateVersionFlow useRestApiVersion: $useRestApiVersion" }
            if (useRestApiVersion) {
                lanUpdateVersionProvider.get()
            } else {
                updateStatusFlow
                    .map { status -> status.check.availableVersion }
                    .filter { versionString -> versionString.isNotBlank() }
                    .filter { versionString -> versionString.isNotEmpty() }
                    .map(BsbUpdateVersion::Default)
            }
        }
        .onEach { info { "#updateVersionFlow: $it" } }
        .shareIn(scope, SharingStarted.Lazily, 1)
        .asFlow()
        .wrap()

    override val updateVersionChangelog: WrappedFlow<String> = updateVersionFlow
        .filterNotNull()
        .distinctUntilChanged()
        .map { busyBarVersion ->
            when (busyBarVersion) {
                is BsbUpdateVersion.Default -> {
                    exponentialRetry {
                        rpcFeatureApi.fRpcUpdaterApi
                            .getUpdateChangelog(busyBarVersion.version)
                            .map(GetUpdateChangelog200Response::changelog)
                    }
                }

                is BsbUpdateVersion.Url -> {
                    busyBarVersion.changelog
                }
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
                ?.await()
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
