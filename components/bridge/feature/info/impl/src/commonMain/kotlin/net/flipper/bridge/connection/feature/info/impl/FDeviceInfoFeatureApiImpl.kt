package net.flipper.bridge.connection.feature.info.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.info.api.model.BSBDeviceInfo
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error

@Inject
class FDeviceInfoFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val connectedDevice: FConnectedDeviceApi,
    @Assisted private val fEventsFeatureApi: FEventsFeatureApi?
) : FDeviceInfoFeatureApi, LogTagProvider {
    override val TAG = "FDeviceInfoFeatureApi"

    override suspend fun getDeviceInfo(): BSBDeviceInfo? {
        val statusSystem = rpcFeatureApi.fRpcSystemApi
            .getStatusSystem()
            .getOrNull()
            ?: return null

        return BSBDeviceInfo(
            version = statusSystem.version
        )
    }

    private fun getDeviceNameChangeEventFlow(): Flow<Unit> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.DEVICE_NAME)
            ?.merge(flowOf(Unit))
            .orEmpty()
    }

    override fun getDeviceName(): WrappedFlow<String> {
        return flow {
            emit(connectedDevice.deviceName)
            getDeviceNameChangeEventFlow()
                .mapNotNull {
                    rpcFeatureApi
                        .fRpcSettingsApi
                        .getName()
                        .onFailure { error { "Failed get device name: ${it.message}" } }
                        .getOrNull()
                        ?.name
                }
                .collect { deviceName -> emit(deviceName) }
        }.wrap()
    }

    @Inject
    class InternalFactory(
        private val factory: (FRpcFeatureApi, FConnectedDeviceApi, FEventsFeatureApi?) -> FDeviceInfoFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi,
            connectedDevice: FConnectedDeviceApi,
            fEventsFeatureApi: FEventsFeatureApi?
        ): FDeviceInfoFeatureApiImpl = factory(rpcFeatureApi, connectedDevice, fEventsFeatureApi)
    }
}
