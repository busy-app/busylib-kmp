package net.flipper.bridge.connection.feature.oncall.impl

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.onLatest
import net.flipper.core.busylib.log.LogTagProvider

@AssistedInject
class FOnCallFeatureApiImpl(
    @Assisted rpcFeatureApi: FRpcFeatureApi,
    @Assisted scope: CoroutineScope
) : FOnCallFeatureApi, LogTagProvider {

    override val TAG: String = "FOnCallFeatureApi"

    private val displayLoop = OnCallDisplayLoop(rpcFeatureApi.fRpcAssetsApi)
    private val isOnCallFlow = MutableStateFlow(false)

    init {
        isOnCallFlow
            .dropWhile { isOnCall -> !isOnCall }
            .onLatest { isOnCall ->
                if (isOnCall) displayLoop.run() else displayLoop.clear()
            }
            .launchIn(scope)
    }

    override fun start() {
        isOnCallFlow.value = true
    }

    override fun stop() {
        isOnCallFlow.value = false
    }

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.ON_CALL)
    class FOnCallFeatureFactoryImpl : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null

            return FOnCallFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                scope = scope
            )
        }
    }
}
