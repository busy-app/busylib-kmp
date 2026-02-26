package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AutoUpdate
import net.flipper.bridge.connection.feature.rpc.api.model.GetUpdateChangelogResponse
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.hasCapability
import net.flipper.bsb.cloud.rest.api.BusyFirmwareDirectoryApi
import net.flipper.bsb.cloud.rest.model.BsbFirmwareUpdateFileType
import net.flipper.bsb.cloud.rest.model.BsbFirmwareUpdateTarget
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.DefaultConsumable
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orElse
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Suppress("UnusedPrivateProperty")
class FFirmwareUpdateFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
    private val deviceApi: FConnectedDeviceApi,
    private val busyFirmwareDirectoryApi: BusyFirmwareDirectoryApi,
    private val fDeviceInfoFeatureApi: FDeviceInfoFeatureApi
) : FFirmwareUpdateFeatureApi, LogTagProvider {
    override val TAG: String = "FFirmwareUpdateFeatureApi"

    override val updateStatusFlow: WrappedSharedFlow<UpdateStatus> = fEventsFeatureApi
        ?.getUpdateFlow(UpdateEvent.UPDATER_UPDATE_STATUS)
        .orEmpty()
        .merge(flowOf(DefaultConsumable(false)))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                val couldConsume = consumable.tryConsume()
                exponentialRetry {
                    rpcFeatureApi.fRpcUpdaterApi
                        .getUpdateStatus(couldConsume)
                        .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
                }
            }
        }
        .wrap()

    override suspend fun setAutoUpdate(isEnabled: Boolean): CResult<Unit> {
        val request = AutoUpdate(
            isEnabled = isEnabled,
            intervalStart = DEFAULT_AUTO_UPDATE_INTERVAL_START,
            intervalEnd = DEFAULT_AUTO_UPDATE_INTERVAL_END
        )
        return rpcFeatureApi.fRpcUpdaterApi.setAutoUpdate(request)
            .map { }
            .toCResult()
    }

    override suspend fun getAutoUpdate(): CResult<Boolean> {
        return rpcFeatureApi.fRpcUpdaterApi.getAutoUpdate()
            .map { it.isEnabled }
            .toCResult()
    }

    private suspend fun requireVersionFromRestApi(): BsbUpdateVersion.Url {
        return exponentialRetry {
            runCatching {
                val bsbFirmwareUpdateVersion = busyFirmwareDirectoryApi.getFirmwareDirectory()
                    .getOrThrow()
                    .channels
                    .firstOrNull { channel -> channel.id == "development" } // todo currently we need only development
                    ?.versions
                    ?.maxByOrNull { version -> version.timestamp }
                    ?: error("No development version found")
                val updateFile = bsbFirmwareUpdateVersion
                    .files
                    .filter { it.target == BsbFirmwareUpdateTarget.F21 } // todo currently only F21
                    .firstOrNull { it.type == BsbFirmwareUpdateFileType.UPDATE_TGZ }
                    ?: error("No update file found")
                BsbUpdateVersion.Url(
                    version = bsbFirmwareUpdateVersion.version,
                    url = updateFile.url,
                    sha256 = updateFile.sha256,
                    changelog = bsbFirmwareUpdateVersion.changelog
                )
            }.onFailure { t -> error(t) { "#requireVersionFromRestApi could not find version from REST api " } }
        }
    }

    override val updateVersionFlow = fDeviceInfoFeatureApi
        .deviceVersionFlow
        .flatMapLatest { currentVersion ->
            deviceApi
                .tryCast<FHTTPDeviceApi>()
                ?.hasCapability(FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED)
                .orElse { false }
                .distinctUntilChanged()
                .flatMapLatest { useRestApiVersion ->
                    info { "#updateVersionFlow useRestApiVersion: $useRestApiVersion" }
                    if (useRestApiVersion) {
                        flowOf(requireVersionFromRestApi())
                    } else {
                        updateStatusFlow
                            .map { status -> status.check.availableVersion }
                            .filter { versionString -> versionString.isNotBlank() }
                            .filter { versionString -> versionString.isNotEmpty() }
                            .map(BsbUpdateVersion::Default)
                    }
                }
            // todo commented to be able to test download
//                .filter { updateVersion -> updateVersion.version != currentVersion.version }
        }
        .onEach { info { "#updateVersionFlow: $it" } }
        .shareIn(scope, SharingStarted.Lazily, 1)

    override val updateVersionChangelog: Flow<String> = updateVersionFlow
        .distinctUntilChanged()
        .map { busyBarVersion ->
            when (busyBarVersion) {
                is BsbUpdateVersion.Default -> {
                    exponentialRetry {
                        rpcFeatureApi.fRpcUpdaterApi
                            .getUpdateChangelog(busyBarVersion.version)
                            .map(GetUpdateChangelogResponse::changelog)
                    }
                }

                is BsbUpdateVersion.Url -> {
                    busyBarVersion.changelog
                }
            }
        }
        .onEach { info { "#updateVersionChangelog: $it" } }
        .shareIn(scope, SharingStarted.Lazily, 1)

    @Inject
    class FDeviceFeatureApiFactory(
        private val busyFirmwareDirectoryApi: BusyFirmwareDirectoryApi
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
            val fEventsFeatureApi = unsafeFeatureDeviceApi
                .get(FEventsFeatureApi::class)
                ?.await()
            val fDeviceInfoFeatureApi = unsafeFeatureDeviceApi
                .get(FDeviceInfoFeatureApi::class)
                ?.await()
                ?: return null
            return FFirmwareUpdateFeatureApiImpl(
                rpcFeatureApi = rpcApi,
                fEventsFeatureApi = fEventsFeatureApi,
                scope = scope,
                busyFirmwareDirectoryApi = busyFirmwareDirectoryApi,
                deviceApi = connectedDevice,
                fDeviceInfoFeatureApi = fDeviceInfoFeatureApi
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
