package net.flipper.bridge.connection.feature.ble.api

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error

@AssistedInject
class FBleFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val fEventsFeatureApi: FEventsFeatureApi?,
    @Assisted private val scope: CoroutineScope
) : FBleFeatureApi, LogTagProvider {
    override val TAG: String = "FBleFeatureApi"

    private val bleStatusSharedFlow = fEventsFeatureApi.get(
        scope = scope,
        initial = {
            rpcFeatureApi.fRpcBleApi
                .getBleStatus(false)
                .onFailure { error(it) { "Failed to get Ble status" } }
                .map { response -> response.toEvent() }
        },
        mapper = { flow -> flow.map { it.toPublic() } }
    ).asFlow().wrap()

    override fun getBleStatus(): WrappedFlow<FBleStatus> {
        return bleStatusSharedFlow
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi,
            fEventsFeatureApi: FEventsFeatureApi?,
            scope: CoroutineScope
        ): FBleFeatureApiImpl
    }

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding = binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.BLE)
    class FBleFeatureFactoryImpl : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            val fEventsFeatureApi = unsafeFeatureDeviceApi
                .get(FEventsFeatureApi::class)
                ?.await()

            return FBleFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                fEventsFeatureApi = fEventsFeatureApi,
                scope = scope
            )
        }
    }
}
