package net.flipper.bridge.connection.feature.link.check.onready.api

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.getOrCreate
import net.flipper.bridge.connection.feature.link.check.status.api.LinkedAccountInfoApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(
    BusyLibGraph::class,
    FOnDeviceReadyFeatureApi.Factory::class,
    multibinding = true
)
class FLinkInfoOnReadyFeatureFactoryImpl(
    private val fLinkInfoOnReadyFeatureApiImpl: FLinkInfoOnReadyFeatureApiImpl.InternalFactory,
    private val linkedAccountInfoProviderApi: () -> LinkedAccountInfoApi
) : FOnDeviceReadyFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FOnDeviceReadyFeatureApi? {
        val fRpcCriticalFeatureApi = unsafeFeatureDeviceApi
            .getUnsafe(FRpcCriticalFeatureApi::class)
            ?.await()
            ?: return null
        val linkedAccountInfoApi = unsafeFeatureDeviceApi
            .instanceKeeper
            .getOrCreate(linkedAccountInfoProviderApi)
        return fLinkInfoOnReadyFeatureApiImpl(
            rpcFeatureApi = fRpcCriticalFeatureApi,
            scope = scope,
            linkedAccountInfoApi = linkedAccountInfoApi
        )
    }
}
