package net.flipper.bridge.connection.feature.ble.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import dev.zacsweers.metro.ContributesTo
import kotlin.time.Duration.Companion.seconds

class FBleFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
    private val scope: CoroutineScope
) : FBleFeatureApi, LogTagProvider {
    override val TAG: String = "FBleFeatureApi"

    private fun BleStatusResponse.toFBleStatus(): FBleStatus {
        return when (this.state) {
            BleStatusResponse.State.RESET -> FBleStatus.Reset
            BleStatusResponse.State.INITIALIZATION -> FBleStatus.Initialization
            BleStatusResponse.State.DISABLED -> FBleStatus.Disabled
            BleStatusResponse.State.ENABLED -> FBleStatus.Enabled
            BleStatusResponse.State.CONNECTABLE -> {
                FBleStatus.Connectable
            }

            BleStatusResponse.State.CONNECTED -> {
                FBleStatus.Connected(
                    address = this.address ?: return FBleStatus.Enabled,
                )
            }

            BleStatusResponse.State.INTERNAL_ERROR -> FBleStatus.InternalError
        }
    }

    private val bleStatusSharedFlow = flow {
        val status = exponentialRetry {
            rpcFeatureApi.fRpcBleApi
                .getBleStatus(false)
                .onFailure { error(it) { "Failed to get Ble status" } }
                .map { response -> response.toFBleStatus() }
        }
        emit(status)
    }.shareIn(scope, SharingStarted.WhileSubscribed(30.seconds), 1).asFlow().wrap()

    override fun getBleStatus(): WrappedFlow<FBleStatus> {
        return bleStatusSharedFlow
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
                fEventsFeatureApi = fEventsFeatureApi,
                scope = scope
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface FBleFeatureComponent {
        @Provides
        @IntoMap
        @FDeviceFeatureKey(FDeviceFeature.BLE)
        fun provideFBleFeatureFactory(
            fBleFeatureFactory: FBleFeatureFactoryImpl
        ): FDeviceFeatureApi.Factory {
            return fBleFeatureFactory
        }
    }
}
