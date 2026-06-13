package net.flipper.bridge.connection.feature.link.check.onready.api

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph

@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<FOnDeviceReadyFeatureApi.Factory>())
@ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
@FDeviceFeatureKey(FDeviceFeature.LINKED_USER_STATUS)
class FLinkInfoOnReadyFeatureFactoryImpl(
    private val fLinkInfoOnReadyFeatureApiImpl: FLinkInfoOnReadyFeatureApiImpl.Factory,
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
