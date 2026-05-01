package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.BatteryStatus
import BSB_State.Power
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.core.busylib.data.Fraction

object PowerProtobufMapper {
    fun map(power: Power): BusyLibUpdateEvent.Power {
        val known = power.known ?: return BusyLibUpdateEvent.Power.Unknown
        return BusyLibUpdateEvent.Power.Provided(
            chargePercent = Fraction.fromWholePercent(known.battery_charge_percent),
            status = when (known.battery_status) {
                BatteryStatus.CHARGED -> BusyLibUpdateEvent.Power.Provided.Status.CHARGED
                BatteryStatus.CHARGING -> BusyLibUpdateEvent.Power.Provided.Status.CHARGING
                BatteryStatus.DISCHARGING -> BusyLibUpdateEvent.Power.Provided.Status.DISCHARGING
                is BatteryStatus.Unrecognized -> null
            }
        )
    }
}
