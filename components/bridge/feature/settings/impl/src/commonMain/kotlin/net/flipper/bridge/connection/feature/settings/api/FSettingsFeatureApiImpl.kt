package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
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
import net.flipper.bridge.connection.feature.rpc.api.model.toBsbBrightnessInfo
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FSettingsFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val fEventsFeatureApi: FEventsFeatureApi?
) : FSettingsFeatureApi, LogTagProvider {
    override val TAG: String = "FSettingsFeatureApi"

    override fun getBrightnessInfoFlow(): Flow<BsbBrightnessInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.BRIGHTNESS)
            .orEmpty()
            .merge(flowOf(Unit))
            .map {
                exponentialRetry {
                    rpcFeatureApi
                        .fRpcSettingsApi
                        .getDisplayBrightness()
                        .onFailure { error(it) { "Failed to get Settings status" } }
                }
            }
            .map { brightnessInfo -> brightnessInfo.toBsbBrightnessInfo() }
            .wrap()
    }

    override fun getVolumeFlow(): Flow<AudioVolumeInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.AUDIO_VOLUME)
            .orEmpty()
            .merge(flowOf(Unit))
            .map {
                exponentialRetry {
                    rpcFeatureApi.fRpcSettingsApi
                        .getAudioVolume()
                        .onFailure { error(it) { "Failed to get Settings status" } }
                }
            }
            .wrap()
    }

    override suspend fun setVolume(volume: Int): Result<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setAudioVolume(volume)
            .map { }
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
                fEventsFeatureApi = fEventsFeatureApi
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
