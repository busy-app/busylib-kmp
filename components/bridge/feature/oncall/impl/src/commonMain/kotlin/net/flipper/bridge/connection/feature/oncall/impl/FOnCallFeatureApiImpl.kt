package net.flipper.bridge.connection.feature.oncall.impl

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.log.LogTagProvider

@AssistedInject
class FOnCallFeatureApiImpl(
    @Assisted rpcFeatureApi: FRpcFeatureApi,
    @Assisted scope: CoroutineScope
) : FOnCallFeatureApi, LogTagProvider {

    override val TAG: String = "FOnCallFeatureApi"

    private val displayLoop = OnCallDisplayLoop(rpcFeatureApi.fRpcAssetsApi)
    private val singleJobScope = scope.asSingleJobScope()

    override fun start() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            displayLoop.run()
        }
    }

    override fun stop() {
        singleJobScope.cancelPrevious()
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
