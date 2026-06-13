package net.flipper.bridge.connection.feature.hardwareid.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
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
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.verbose

class FHardwareIdFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val httpDevice: FHTTPDeviceApi
) : FHardwareIdFeatureApi, LogTagProvider {
    override val TAG = "FHardwareIdFeatureApi"

    override fun getHardwareIdFlow(): WrappedFlow<String?> = httpDevice
        .hasCapability(FHTTPTransportCapability.BB_LOCAL_CONNECTION)
        .distinctUntilChanged()
        .mapLatest { hasLocalCapability ->
            if (hasLocalCapability) {
                exponentialRetry {
                    rpcFeatureApi
                        .fRpcSystemApi
                        .getDeviceStatus(localOnly = true)
                        .map { deviceStatus -> deviceStatus.serialNumber }
                        .onFailure { error(it) { "Failed to read device status for hardware id, retrying" } }
                }
            } else {
                verbose { "Hardware id isn't provided because no local connection" }
                null
            }
        }.wrap()

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding = binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.HARDWARE_ID)
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
}
