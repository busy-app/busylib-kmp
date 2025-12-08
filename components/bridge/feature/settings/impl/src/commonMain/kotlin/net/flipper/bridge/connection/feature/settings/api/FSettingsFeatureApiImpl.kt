package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration.Companion.seconds

private val POOLING_TIME = 3.seconds

@Inject
class FSettingsFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
) : FSettingsFeatureApi, LogTagProvider {
    override val TAG: String = "FSettingsFeatureApi"

    override fun getBrightnessInfoFlow(): Flow<DisplayBrightnessInfo> {
        return callbackFlow {
            while (isActive) {
                val status = exponentialRetry {
                    rpcFeatureApi.getBrightnessInfo()
                        .onFailure { error(it) { "Failed to get Settings status" } }
                }
                send(status)
                delay(POOLING_TIME)
            }
        }.wrap()
    }

    override fun getVolumeFlow(): Flow<AudioVolumeInfo> {
        return callbackFlow {
            while (isActive) {
                val status = exponentialRetry {
                    rpcFeatureApi.getVolumeInfo()
                        .onFailure { error(it) { "Failed to get Settings status" } }
                }
                send(status)
                delay(POOLING_TIME)
            }
        }.wrap()
    }

    @Inject
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .getUnsafe(FRpcFeatureApi::class)
                ?.await()
                ?: return null

            return FSettingsFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi
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
