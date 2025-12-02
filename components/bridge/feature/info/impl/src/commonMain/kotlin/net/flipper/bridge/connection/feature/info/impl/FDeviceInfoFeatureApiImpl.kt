package net.flipper.bridge.connection.feature.info.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.info.api.model.BSBDeviceInfo
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
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
        val statusSystem = rpcFeatureApi.getStatusSystem().getOrNull() ?: return null

        return BSBDeviceInfo(
            version = statusSystem.version
        )
    }

    override fun getDeviceName(scope: CoroutineScope): StateFlow<String> {
        return flow {
            emit(Unit)
            fEventsFeatureApi
                ?.getUpdateFlow(UpdateEvent.DEVICE_NAME)
                ?.collect {
                    emit(Unit)
                }
        }.mapNotNull {
            rpcFeatureApi.getDeviceName()
                .onFailure {
                    error { "Failed get device name: ${it.message}" }
                }
                .getOrNull()
        }.stateIn(scope, SharingStarted.WhileSubscribed(), connectedDevice.deviceName)
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
