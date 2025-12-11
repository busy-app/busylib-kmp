package net.flipper.bridge.connection.feature.ble.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BleState
import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FBleFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val fEventsFeatureApi: FEventsFeatureApi?
) : FBleFeatureApi, LogTagProvider {
    override val TAG: String = "FBleFeatureApi"

    private fun BleStatusResponse.toFBleStatus(): FBleStatus {
        return when (this.state) {
            BleState.DISABLED -> FBleStatus.Disabled
            BleState.ENABLED -> FBleStatus.Enabled
            BleState.CONNECTED -> {
                FBleStatus.Connected(
                    connectedDeviceName = connectedDeviceName
                        ?: return FBleStatus.Enabled,
                    connectedDeviceBssid = connectedDeviceBssid
                        ?: return FBleStatus.Enabled
                )
            }
        }
    }

    override fun getBleStatus(): WrappedFlow<FBleStatus> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.BRIGHTNESS)
            .orEmpty()
            .merge(flowOf(Unit))
            .map {
                exponentialRetry {
                    rpcFeatureApi.getBleStatus()
                        .onFailure { error(it) { "Failed to get Ble status" } }
                        .map { response -> response.toFBleStatus() }
                }
            }
            .wrap()
    }

    @Inject
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
                fEventsFeatureApi = fEventsFeatureApi
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface FBleFeatureComponent {
        @Provides
        @IntoMap
        fun provideFBleFeatureFactory(
            fBleFeatureFactory: FBleFeatureFactoryImpl
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.BLE to fBleFeatureFactory
        }
    }
}
