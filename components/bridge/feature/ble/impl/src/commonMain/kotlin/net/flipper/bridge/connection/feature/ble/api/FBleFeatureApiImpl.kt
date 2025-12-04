package net.flipper.bridge.connection.feature.ble.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BleState
import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration.Companion.seconds

private val POOLING_TIME = 3.seconds

@Inject
class FBleFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
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
        return callbackFlow {
            while (isActive) {
                val status = rpcFeatureApi.getBleStatus()
                    .onFailure { error(it) { "Failed to get Ble status" } }
                    .getOrNull()
                    ?.toFBleStatus() ?: continue
                send(status)
                delay(POOLING_TIME)
            }
        }.wrap()
    }

    @Inject
    class FBleFeatureFactoryImpl : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .getUnsafe(FRpcFeatureApi::class)
                ?.await()
                ?: return null

            return FBleFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi
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
