package net.flipper.bridge.connection.feature.info.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.info.mapper.toBsbBusyBarStatusSystem
import net.flipper.bridge.connection.feature.info.mapper.toBsbStatusFirmware
import net.flipper.bridge.connection.feature.info.model.BsbBusyBarStatusSystem
import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion
import net.flipper.bridge.connection.feature.info.model.BsbStatusFirmware
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrapFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider

class FDeviceInfoFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val scope: CoroutineScope,
) : FDeviceInfoFeatureApi, LogTagProvider {
    override val TAG = "FDeviceInfoFeatureApi"

    override suspend fun getDeviceInfo(): CResult<BsbBusyBarStatusSystem> {
        return rpcFeatureApi.fRpcSystemApi.getStatusSystem()
            .map { busyBarStatusSystem -> busyBarStatusSystem.toBsbBusyBarStatusSystem() }
            .toCResult()
    }

    override suspend fun getDeviceFirmware(): CResult<BsbStatusFirmware> {
        return rpcFeatureApi.fRpcSystemApi.getStatusFirmware()
            .map { statusFirmware -> statusFirmware.toBsbStatusFirmware() }
            .toCResult()
    }

    override val deviceVersionFlow: WrappedFlow<BsbBusyBarVersion> = flow {
        val statusFirmware = exponentialRetry {
            rpcFeatureApi
                .fRpcSystemApi
                .getStatusFirmware()
                .map { statusFirmware -> statusFirmware.toBsbStatusFirmware() }
        }
        val version = statusFirmware
            .version
            .let(::BsbBusyBarVersion)
        emit(version)
    }.shareIn(scope, SharingStarted.Lazily, 1).wrapFlow()

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.DEVICE_INFO)
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
            return FDeviceInfoFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                scope = scope,
            )
        }
    }
}
