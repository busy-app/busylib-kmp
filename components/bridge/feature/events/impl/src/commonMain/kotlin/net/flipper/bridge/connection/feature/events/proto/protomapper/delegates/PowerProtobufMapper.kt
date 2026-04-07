package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.BatteryStatus
import BSB_State.Power
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.core.busylib.data.Fraction

object PowerProtobufMapper {
    fun map(power: Power): BusyLibUpdateEvent.Power? {
        val known = power.known ?: return null
        return BusyLibUpdateEvent.Power(
            batteryChargePercent = Fraction.fromWholePercent(known.battery_charge_percent),
            isCharging = known.battery_status == BatteryStatus.CHARGING,
        )
    }
}
