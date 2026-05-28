package net.flipper.bridge.connection.feature.hardwareid.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.hardwareid.api.FHardwareIdFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.hasCapability
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FHardwareIdFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val httpDevice: FHTTPDeviceApi
) : FHardwareIdFeatureApi, LogTagProvider {
    override val TAG = "FHardwareIdFeatureApi"

    override fun getHardwareIdFlow(): WrappedFlow<String?> = httpDevice
        .hasCapability(FHTTPTransportCapability.BB_LOCAL_CONNECTION)
        .distinctUntilChanged()
        .map { hasLocalCapability ->
            if (hasLocalCapability) {
                exponentialRetry {
                    rpcFeatureApi
                        .fRpcSystemApi
                        .getDeviceStatus(localOnly = true)
                        .map { deviceStatus -> deviceStatus.serialNumber }
                }
            } else {
                verbose { "Hardware id isn't provided because no local connection" }
                null
            }
        }.wrap()

    @Inject
    class FDeviceFeatureApiFactory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            val httpDevice = connectedDevice as? FHTTPDeviceApi
                ?: return null
            return FHardwareIdFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                httpDevice = httpDevice
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface FFeatureComponent {
        @Provides
        @IntoMap
        fun provideFeatureFactory(
            fDeviceFeatureApiFactory: FDeviceFeatureApiFactory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.HARDWARE_ID to fDeviceFeatureApiFactory
        }
    }
}
