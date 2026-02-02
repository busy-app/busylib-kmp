package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.api.model.toBsbBrightnessInfo
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.DefaultConsumable
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
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

    override fun getBrightnessInfoFlow(): WrappedFlow<BsbBrightnessInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.BRIGHTNESS)
            .orEmpty()
            .merge(flowOf(DefaultConsumable(false)))
            .transformWhileSubscribed(scope = scope) { flow ->
                flow.throttleLatest { consumable ->
                    val couldConsume = consumable.tryConsume()
                    exponentialRetry {
                        rpcFeatureApi
                            .fRpcSettingsApi
                            .getDisplayBrightness(couldConsume)
                            .map { it.toBsbBrightnessInfo() }
                            .onFailure { error(it) { "Failed to get Settings status" } }
                    }
                }
            }
            .map { value -> value }
            .wrap()
    }

    override fun getVolumeFlow(): WrappedFlow<AudioVolumeInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.AUDIO_VOLUME)
            .orEmpty()
            .merge(flowOf(DefaultConsumable(false)))
            .transformWhileSubscribed(scope = scope) { flow ->
                flow.throttleLatest { consumable ->
                    val couldConsume = consumable.tryConsume()
                    exponentialRetry {
                        info { "#getVolumeFlow getting volume flow" }
                        rpcFeatureApi.fRpcSettingsApi
                            .getAudioVolume(couldConsume)
                            .onFailure { error(it) { "Failed to get Settings status" } }
                    }
                }
            }
            .map { value -> value }
            .wrap()
    }

    override suspend fun setVolume(volume: Int): Result<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setAudioVolume(volume)
            .map { }
    }

    override fun getDeviceName(): WrappedFlow<String> {
        return flow {
            emit(connectedDevice.deviceName)
            fEventsFeatureApi
                ?.getUpdateFlow(UpdateEvent.DEVICE_NAME)
                .orEmpty()
                .merge(flowOf(DefaultConsumable(false)))
                .transformWhileSubscribed(scope = scope) { flow ->
                    flow.throttleLatest { consumable ->
                        val couldConsume = consumable.tryConsume()
                        exponentialRetry {
                            rpcFeatureApi
                                .fRpcSettingsApi
                                .getName(couldConsume)
                                .map { nameInfo -> nameInfo.name }
                        }
                    }
                }
                .collect { deviceName -> emit(deviceName) }
        }.wrap()
    }

    override suspend fun setDeviceName(name: String): Result<Unit> {
        return rpcFeatureApi.fRpcSettingsApi.setName(NameInfo(name)).map { }
    }

    override suspend fun setBrightness(
        front: BsbBrightness,
        back: BsbBrightness
    ): Result<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setDisplayBrightness(front = front, back = back)
            .map { }
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
