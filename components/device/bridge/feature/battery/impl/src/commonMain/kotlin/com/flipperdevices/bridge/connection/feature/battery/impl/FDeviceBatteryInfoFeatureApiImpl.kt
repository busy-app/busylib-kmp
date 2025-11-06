package com.flipperdevices.bridge.connection.feature.battery.impl

import com.flipperdevices.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import com.flipperdevices.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.PowerState
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import com.flipperdevices.core.ktx.common.orEmpty
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
                val isCharging = byteArray?.firstOrNull()
                    ?.and(BATTERY_POWER_STATE_MASK)
                    ?.equals(BATTERY_POWER_STATE_MASK)
                    ?: return@map null
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
        return flow { emit(rpcFeatureApi.getStatusPower().getOrNull()) }
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

    override suspend fun getDeviceBatteryInfo(): Flow<BSBDeviceBatteryInfo> {
        return getGattBatteryInfoFlow()
            .flatMapLatest { batteryInfo ->
                if (batteryInfo == null) {
                    getRpcBatteryInfoFlow()
                } else {
                    flowOf(batteryInfo)
                }
            }
    }

    @AssistedFactory
    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi,
            metaInfoApi: FTransportMetaInfoApi?
        ): FDeviceBatteryInfoFeatureApiImpl
    }

    companion object {
        private const val MAX_BATTERY_LEVEL = 100
        const val BATTERY_POWER_STATE_MASK: Byte = 0b0011_0000
    }
}
