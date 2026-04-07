package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.settings.mapper.toBsbBrightnessInfo
import net.flipper.bridge.connection.feature.settings.mapper.toBsbVolume
import net.flipper.bridge.connection.feature.settings.mapper.toDisplayBrightnessInfo
import net.flipper.bridge.connection.feature.settings.model.BsbBrightness
import net.flipper.bridge.connection.feature.settings.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.settings.model.BsbVolume
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.data.Fraction
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FSettingsFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
    private val connectedDevice: FConnectedDeviceApi,
) : FSettingsFeatureApi, LogTagProvider {
    override val TAG: String = "FSettingsFeatureApi"

    private val deviceNameFlow = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.DeviceName>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                when (consumable) {
                    ConsumableUpdateEvent.Empty -> {
                        val couldConsume = consumable.tryConsume()
                        exponentialRetry {
                            rpcFeatureApi
                                .fRpcSettingsApi
                                .getName(couldConsume)
                                .map { nameInfo -> nameInfo.name }
                        }
                    }

                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.DeviceName> -> {
                        consumable.busyLibUpdateEvent.deviceName
                    }
                }
            }
        }.stateIn(scope, SharingStarted.Lazily, connectedDevice.deviceName)

    private val brightnessInfoFlow = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.Brightness>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                val couldConsume = consumable.tryConsume()
                when (consumable) {
                    ConsumableUpdateEvent.Empty -> {
                        exponentialRetry {
                            rpcFeatureApi
                                .fRpcSettingsApi
                                .getDisplayBrightness(couldConsume)
                                .map { displayBrightnessInfo -> displayBrightnessInfo.toBsbBrightnessInfo() }
                                .onFailure { error(it) { "Failed to get Settings status" } }
                        }
                    }

                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.Brightness> -> {
                        consumable.busyLibUpdateEvent.bsbBrightnessInfo
                    }
                }
            }
        }
        .asFlow()
        .wrap()

    override fun getBrightnessInfoFlow(): WrappedFlow<BsbBrightnessInfo> {
        return brightnessInfoFlow
    }

    private val volumeFlow = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.Volume>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                val couldConsume = consumable.tryConsume()
                when (consumable) {
                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.Volume> -> {
                        BsbVolume(consumable.busyLibUpdateEvent.volume)
                    }

                    else -> {
                        exponentialRetry {
                            rpcFeatureApi.fRpcSettingsApi
                                .getAudioVolume(couldConsume)
                                .map { audioVolumeInfo -> audioVolumeInfo.toBsbVolume() }
                        }
                    }
                }
            }
        }
        .asFlow()
        .wrap()

    override fun getVolumeFlow(): WrappedFlow<BsbVolume> {
        return volumeFlow
    }

    override suspend fun setVolume(volume: Fraction): CResult<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setAudioVolume(volume.toWholePercent().toInt())
            .map { }
            .onSuccess {
                val model = AudioVolumeInfo(volume)
                val event = BusyLibUpdateEvent.Volume(model.volume)
                fEventsFeatureApi?.onBusyLibEvent(event)
            }
            .toCResult()
    }

    override fun getDeviceName(): WrappedStateFlow<String> {
        return deviceNameFlow.wrap()
    }

    override suspend fun setDeviceName(name: String): CResult<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setName(NameInfo(name))
            .map { }
            .onSuccess {
                val model = name
                val event = BusyLibUpdateEvent.DeviceName(model)
                fEventsFeatureApi?.onBusyLibEvent(event)
            }
            .toCResult()
    }

    override suspend fun setBrightness(
        value: BsbBrightness,
    ): CResult<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setDisplayBrightness(value.toDisplayBrightnessInfo())
            .map { }
            .onSuccess {
                val model = BsbBrightnessInfo(value)
                val event = BusyLibUpdateEvent.Brightness(model)
                fEventsFeatureApi?.onBusyLibEvent(event)
            }
            .toCResult()
    }

    @Inject
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            val fEventsFeatureApi = unsafeFeatureDeviceApi
                .get(FEventsFeatureApi::class)
                ?.await()

            return FSettingsFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                fEventsFeatureApi = fEventsFeatureApi,
                connectedDevice = connectedDevice,
                scope = scope
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface Component {
        @Provides
        @IntoMap
        fun provideFeatureFactory(
            factory: Factory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.SETTINGS to factory
        }
    }
}
