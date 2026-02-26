package net.flipper.bridge.connection.feature.info.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FDeviceInfoFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val scope: CoroutineScope,
) : FDeviceInfoFeatureApi, LogTagProvider {
    override val TAG = "FDeviceInfoFeatureApi"

    override suspend fun getDeviceInfo(): CResult<BusyBarStatusSystem> {
        return rpcFeatureApi.fRpcSystemApi.getStatusSystem().toCResult()
    }

    override val deviceVersionFlow: Flow<BusyBarVersion> = flow {
        val version = exponentialRetry {
            rpcFeatureApi
                .fRpcSystemApi
                .getStatusFirmware()
        }.version.let(::BusyBarVersion)
        emit(version)
    }.shareIn(scope, SharingStarted.Lazily, 1)

    @Inject
    class FDeviceFeatureApiFactory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            return FDeviceInfoFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                scope = scope,
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
            return FDeviceFeature.DEVICE_INFO to fDeviceFeatureApiFactory
        }
    }
}
