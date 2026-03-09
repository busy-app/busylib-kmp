package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.api.model.toBsbBrightnessInfo
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FSettingsFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
    private val connectedDevice: FConnectedDeviceApi,
) : FSettingsFeatureApi, LogTagProvider {
    override val TAG: String = "FSettingsFeatureApi"

    private val deviceNameFlow = fEventsFeatureApi
        ?.getUpdateFlow(BsbUpdateEvent.DEVICE_NAME)
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                when (consumable) {
                    is ConsumableUpdateEvent.Bsb,
                    ConsumableUpdateEvent.Empty -> {
                        val couldConsume = consumable.tryConsume()
                        exponentialRetry {
                            rpcFeatureApi
                                .fRpcSettingsApi
                                .getName(couldConsume)
                                .map { nameInfo -> nameInfo.name }
                        }
                    }

                    is ConsumableUpdateEvent.BusyLib<*> -> {
                        consumable.busyLibUpdateEvent
                            .tryCast<BusyLibUpdateEvent.DeviceName>()
                            ?.deviceName
                    }
                }
            }.filterNotNull()
        }.stateIn(scope, SharingStarted.Lazily, connectedDevice.deviceName)

    override fun getBrightnessInfoFlow(): WrappedFlow<BsbBrightnessInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(BsbUpdateEvent.BRIGHTNESS)
            .orEmpty()
            .merge(flowOf(ConsumableUpdateEvent.Empty))
            .transformWhileSubscribed(scope = scope) { flow ->
                flow.throttleLatest { consumable ->
                    val couldConsume = consumable.tryConsume()
                    when (consumable) {
                        ConsumableUpdateEvent.Empty,
                        is ConsumableUpdateEvent.Bsb -> {
                            exponentialRetry {
                                rpcFeatureApi
                                    .fRpcSettingsApi
                                    .getDisplayBrightness(couldConsume)
                                    .map { it.toBsbBrightnessInfo() }
                                    .onFailure { error(it) { "Failed to get Settings status" } }
                            }
                        }

                        is ConsumableUpdateEvent.BusyLib<*> -> {
                            consumable.busyLibUpdateEvent
                                .tryCast<BusyLibUpdateEvent.Brightness>()
                                ?.bsbBrightnessInfo
                        }
                    }
                }.filterNotNull()
            }
            .asFlow()
            .wrap()
    }

    override fun getVolumeFlow(): WrappedFlow<AudioVolumeInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(BsbUpdateEvent.AUDIO_VOLUME)
            .orEmpty()
            .merge(flowOf(ConsumableUpdateEvent.Empty))
            .transformWhileSubscribed(scope = scope) { flow ->
                flow.throttleLatest { consumable ->
                    when (consumable) {
                        is ConsumableUpdateEvent.Bsb,
                        ConsumableUpdateEvent.Empty -> {
                            val couldConsume = consumable.tryConsume()
                            exponentialRetry {
                                info { "#getVolumeFlow getting volume flow" }
                                rpcFeatureApi.fRpcSettingsApi
                                    .getAudioVolume(couldConsume)
                                    .onFailure { error(it) { "Failed to get Settings status" } }
                            }
                        }

                        is ConsumableUpdateEvent.BusyLib<*> -> {
                            consumable.busyLibUpdateEvent
                                .tryCast<BusyLibUpdateEvent.Volume>()
                                ?.audioVolumeInfo
                        }
                    }
                }.filterNotNull()
            }
            .asFlow()
            .wrap()
    }

    override suspend fun setVolume(volume: Int): CResult<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setAudioVolume(volume)
            .map { }
            .onSuccess {
                val model = AudioVolumeInfo(volume / MAX_PERCENTAGE_VALUE)
                val event = BusyLibUpdateEvent.Volume(model)
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
            .setDisplayBrightness(value = value)
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

    companion object {
        private const val MAX_PERCENTAGE_VALUE = 100.0
    }
}
