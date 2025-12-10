package net.flipper.bridge.connection.feature.link.check.onready.api

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
@ContributesBinding(
    BusyLibGraph::class,
    FOnDeviceReadyFeatureApi.Factory::class,
    multibinding = true
)
class FLinkInfoOnReadyFeatureFactoryImpl(
    private val fLinkInfoOnReadyFeatureApiImpl: FLinkInfoOnReadyFeatureApiImpl.InternalFactory,
) : FOnDeviceReadyFeatureApi.Factory, FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FOnDeviceReadyFeatureApi? {
        val fRpcCriticalFeatureApi = unsafeFeatureDeviceApi
            .get(FRpcCriticalFeatureApi::class)
            ?.await()
            ?: return null
        return fLinkInfoOnReadyFeatureApiImpl(
            rpcFeatureApi = fRpcCriticalFeatureApi,
            scope = scope,
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FLinkInfoOnDemandFeatureComponent {
    @Provides
    @IntoMap
    fun provideFLinkInfoOnDemandFeatureFactory(
        fLinkInfoOnDemandFeatureFactory: FLinkInfoOnReadyFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.LINKED_USER_STATUS to fLinkInfoOnDemandFeatureFactory
    }
}
