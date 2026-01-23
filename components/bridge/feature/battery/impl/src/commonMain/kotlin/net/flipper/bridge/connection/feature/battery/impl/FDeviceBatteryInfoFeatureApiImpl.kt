package net.flipper.bridge.connection.feature.battery.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import net.flipper.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.PowerState
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.orEmpty
import kotlin.experimental.and

@Inject
class FDeviceBatteryInfoFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val metaInfoApi: FTransportMetaInfoApi?,
) : FDeviceBatteryInfoFeatureApi {

    private fun getBatteryLevelFlow(): Flow<Int?> {
        return metaInfoApi
            ?.get(TransportMetaInfoKey.BATTERY_LEVEL)
            ?.getOrNull()
            .orEmpty()
            .map { byteArray ->
                byteArray?.firstOrNull()
                    ?.toFloat()
                    ?.div(MAX_BATTERY_LEVEL)
                    ?.times(other = 100f)
                    ?.toInt()
                    ?.coerceIn(minimumValue = 0, maximumValue = 100)
            }
    }

    private fun getBatteryPowerStateFlow(): Flow<BSBDeviceBatteryInfo.BSBBatteryState?> {
        return metaInfoApi
            ?.get(TransportMetaInfoKey.BATTERY_POWER_STATE)
            ?.getOrNull()
            .orEmpty()
            .map { byteArray ->
                // https://github.com/flipperdevices/bsb-firmware/blob/9acca0c947e764bb0fbdabb4b7b513afa6519de7/applications/services/ble/service/battery/ble_service_battery_i.h#L13
                val stateByte = byteArray?.getOrNull(1) ?: return@map null

                val maskedValue = stateByte and BATTERY_POWER_STATE_MASK
                val isCharging = maskedValue == BATTERY_POWER_STATE_CHARGING

                if (isCharging) {
                    BSBDeviceBatteryInfo.BSBBatteryState.CHARGING
                } else {
                    BSBDeviceBatteryInfo.BSBBatteryState.DISCHARGING
                }
            }
    }

    private fun getGattBatteryInfoFlow(): Flow<BSBDeviceBatteryInfo?> {
        return combine(
            flow = getBatteryLevelFlow(),
            flow2 = getBatteryPowerStateFlow(),
            transform = { level, state ->
                if (level != null && state != null) {
                    BSBDeviceBatteryInfo(
                        state = state,
                        percentage = level
                    )
                } else {
                    null
                }
            }
        )
    }

    private fun getRpcBatteryInfoFlow(): Flow<BSBDeviceBatteryInfo> {
        return flow { emit(rpcFeatureApi.fRpcSystemApi.getStatusPower().getOrNull()) }
            .filterNotNull()
            .map { status ->
                BSBDeviceBatteryInfo(
                    state = when (status.state) {
                        PowerState.DISCHARGING -> BSBDeviceBatteryInfo.BSBBatteryState.DISCHARGING
                        PowerState.CHARGING -> BSBDeviceBatteryInfo.BSBBatteryState.CHARGING
                        PowerState.CHARGED -> BSBDeviceBatteryInfo.BSBBatteryState.CHARGED
                    },
                    percentage = status.batteryCharge
                )
            }
    }

    override fun getDeviceBatteryInfo(): WrappedFlow<BSBDeviceBatteryInfo> {
        return getGattBatteryInfoFlow()
            .flatMapLatest { batteryInfo ->
                if (batteryInfo == null) {
                    getRpcBatteryInfoFlow()
                } else {
                    flowOf(batteryInfo)
                }
            }.wrap()
    }

    @Inject
    class InternalFactory(
        private val factory: (FRpcFeatureApi, FTransportMetaInfoApi?) -> FDeviceBatteryInfoFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi,
            metaInfoApi: FTransportMetaInfoApi?
        ): FDeviceBatteryInfoFeatureApiImpl = factory(rpcFeatureApi, metaInfoApi)
    }

    companion object {
        private const val MAX_BATTERY_LEVEL = 100
        const val BATTERY_POWER_STATE_MASK: Byte = 0b0110_0000
        const val BATTERY_POWER_STATE_CHARGING: Byte = 0b0010_0000
    }
}
