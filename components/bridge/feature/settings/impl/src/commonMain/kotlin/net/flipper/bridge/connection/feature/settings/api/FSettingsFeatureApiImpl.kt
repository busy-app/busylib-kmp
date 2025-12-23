package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.api.model.toBsbBrightnessInfo
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration.Companion.seconds

class FSettingsFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
    private val connectedDevice: FConnectedDeviceApi,
) : FSettingsFeatureApi, LogTagProvider {
    override val TAG: String = "FSettingsFeatureApi"

    private val brightnessSharedFlow = fEventsFeatureApi
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
        .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds), 1)

    override fun getBrightnessInfoFlow(): WrappedSharedFlow<BsbBrightnessInfo> {
        return brightnessSharedFlow.wrap()
    }

    private val volumeSharedFlow = fEventsFeatureApi
        ?.getUpdateFlow(UpdateEvent.AUDIO_VOLUME)
        .orEmpty()
        .merge(flowOf(Unit))
        .map {
            exponentialRetry {
                info { "#getVolumeFlow getting volume flow" }
                rpcFeatureApi.fRpcSettingsApi
                    .getAudioVolume()
                    .onFailure { error(it) { "Failed to get Settings status" } }
            }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds), 1)

    override fun getVolumeFlow(): WrappedSharedFlow<AudioVolumeInfo> {
        return volumeSharedFlow.wrap()
    }

    override suspend fun setVolume(volume: Int): Result<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setAudioVolume(volume)
            .map { }
    }

    private val deviceNameSharedFlow = flow {
        emit(connectedDevice.deviceName)
        fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.DEVICE_NAME)
            ?.merge(flowOf(Unit))
            .orEmpty()
            .mapNotNull {
                exponentialRetry {
                    rpcFeatureApi
                        .fRpcSettingsApi
                        .getName()
                        .map { nameInfo -> nameInfo.name }
                }
            }
            .collect { deviceName -> emit(deviceName) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5.seconds), 1)

    override fun getDeviceName(): WrappedSharedFlow<String> {
        return deviceNameSharedFlow.wrap()
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
