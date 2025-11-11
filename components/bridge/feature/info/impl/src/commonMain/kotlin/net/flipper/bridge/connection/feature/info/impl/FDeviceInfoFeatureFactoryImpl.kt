package net.flipper.bridge.connection.feature.info.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FDeviceInfoFeatureFactoryImpl(
    private val deviceInfoFeatureFactory: FDeviceInfoFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val rpcFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcFeatureApi::class)
            ?.await()
            ?: return null
        return deviceInfoFeatureFactory(
            rpcFeatureApi = rpcFeatureApi
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FDeviceInfoFeatureComponent {
    @Provides
    @IntoMap
    fun provideFDeviceInfoFeatureFactory(
        fDeviceInfoFeatureFactory: FDeviceInfoFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.DEVICE_INFO to fDeviceInfoFeatureFactory
    }
}
