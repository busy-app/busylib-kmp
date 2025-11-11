package net.flipper.bridge.connection.feature.link.check.ondemand.api

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FLinkInfoOnDemandFeatureFactoryImpl(
    private val linkedInfoFeatureFactory: FLinkInfoOnDemandFeatureApiImpl.InternalFactory
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val fRpcCriticalFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcCriticalFeatureApi::class)
            ?.await()
            ?: return null

        return linkedInfoFeatureFactory(
            rpcFeatureApi = fRpcCriticalFeatureApi,
            scope = scope
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FLinkInfoOnDemandFeatureComponent {
    @Provides
    @IntoMap
    fun provideFLinkInfoOnDemandFeatureFactory(
        fLinkInfoOnDemandFeatureFactory: FLinkInfoOnDemandFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.LINKED_USER_STATUS to fLinkInfoOnDemandFeatureFactory
    }
}
