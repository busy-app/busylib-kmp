package net.flipper.bridge.connection.feature.rpc.impl.critical

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph

@Inject
@ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
@FDeviceFeatureKey(FDeviceFeature.RPC_CRITICAL)
class FRpcCriticalFeatureFactoryImpl(
    private val fRpcCriticalFeatureApiFactory: FRpcCriticalFeatureApiImpl.Factory,
) : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val httpClient = connectedDevice as? FHTTPDeviceApi ?: return null
        return fRpcCriticalFeatureApiFactory.invoke(
            client = getHttpClient(httpClient.getDeviceHttpEngine())
        )
    }
}
