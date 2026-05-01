package net.flipper.bridge.connection.feature.battery.impl.delegates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.PowerState
import net.flipper.core.busylib.data.Fraction
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error

class FEventsBatteryInfoDelegate(
    eventsApi: FEventsFeatureApi,
    scope: CoroutineScope,
    rpcFeatureApi: FRpcFeatureApi,
) : LogTagProvider {
    override val TAG = "FEventsBatteryInfoDelegate"
    val rpcBatteryInfoFlow = eventsApi
        .get<BusyLibUpdateEvent.Power, BSBDeviceBatteryInfo?>(
            scope = scope,
            initial = {
                rpcFeatureApi.fRpcSystemApi.getStatusPower().onFailure {
                    error(it) { "Failed to get battery info" }
                }.mapCatching { response ->
                    BusyLibUpdateEvent.Power.Provided(
                        status = when (response.state) {
                            PowerState.DISCHARGING -> BusyLibUpdateEvent.Power.Provided.Status.DISCHARGING
                            PowerState.CHARGING -> BusyLibUpdateEvent.Power.Provided.Status.CHARGING
                            PowerState.CHARGED -> BusyLibUpdateEvent.Power.Provided.Status.CHARGED
                        },
                        chargePercent = Fraction.fromWholePercent(response.batteryCharge)
                    )
                }
            },
            mapper = {
                it.map { event ->
                    when (event) {
                        is BusyLibUpdateEvent.Power.Provided -> {
                            val status = when (event.status) {
                                BusyLibUpdateEvent.Power.Provided.Status.DISCHARGING -> {
                                    BSBDeviceBatteryInfo.BSBBatteryState.DISCHARGING
                                }

                                BusyLibUpdateEvent.Power.Provided.Status.CHARGING -> {
                                    BSBDeviceBatteryInfo.BSBBatteryState.CHARGING
                                }

                                BusyLibUpdateEvent.Power.Provided.Status.CHARGED,
                                null -> BSBDeviceBatteryInfo.BSBBatteryState.CHARGED
                            }
                            BSBDeviceBatteryInfo(
                                state = status,
                                percentage = event.chargePercent
                            )
                        }

                        BusyLibUpdateEvent.Power.Unknown -> null
                    }
                }
            }
        )
}
