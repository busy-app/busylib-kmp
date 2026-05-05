package net.flipper.bridge.connection.feature.battery.impl.delegates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bridge.connection.transport.common.api.meta.getOrNullable
import net.flipper.core.busylib.data.Fraction
import kotlin.experimental.and

class FGATTBatteryInfoDelegate(
    private val metaInfoApi: FTransportMetaInfoApi?,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getBatteryLevelFlow(): Flow<Int?> {
        return metaInfoApi
            .getOrNullable(TransportMetaInfoKey.BATTERY_LEVEL)
            .map { data ->
                val byteArray = (data as? TransportMetaInfoData.RawBytes)?.bytes
                byteArray?.firstOrNull()
                    ?.toFloat()
                    ?.div(MAX_BATTERY_LEVEL)
                    ?.times(other = 100f)
                    ?.toInt()
                    ?.coerceIn(minimumValue = 0, maximumValue = 100)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getBatteryPowerStateFlow(): Flow<BSBDeviceBatteryInfo.BSBBatteryState?> {
        return metaInfoApi
            .getOrNullable(TransportMetaInfoKey.BATTERY_POWER_STATE)
            .map { data ->
                val byteArray = (data as? TransportMetaInfoData.RawBytes)?.bytes
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

    fun getGattBatteryInfoFlow(): Flow<BSBDeviceBatteryInfo?> {
        return combine(
            flow = getBatteryLevelFlow(),
            flow2 = getBatteryPowerStateFlow(),
            transform = { level, state ->
                if (level != null && state != null) {
                    BSBDeviceBatteryInfo(
                        state = state,
                        percentage = Fraction.fromWholePercent(level)
                    )
                } else {
                    null
                }
            }
        )
    }

    companion object {
        private const val MAX_BATTERY_LEVEL = 100
        private const val BATTERY_POWER_STATE_MASK: Byte = 0b0110_0000
        private const val BATTERY_POWER_STATE_CHARGING: Byte = 0b0010_0000
    }
}
