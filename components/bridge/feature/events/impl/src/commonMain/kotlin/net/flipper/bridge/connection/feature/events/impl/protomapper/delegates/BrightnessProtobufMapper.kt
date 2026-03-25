package net.flipper.bridge.connection.feature.events.impl.protomapper.delegates

import BSB_State.Brightness
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.Fraction

object BrightnessProtobufMapper {
    fun map(brightness: Brightness): BusyLibUpdateEvent.Brightness {
        val manual = brightness.manual
        val value = when {
            brightness.automatic != null -> BsbBrightness.Auto
            manual != null -> BsbBrightness.Number(
                Fraction.fromWholePercent(manual.brightness)
            )
            else -> BsbBrightness.Auto
        }
        return BusyLibUpdateEvent.Brightness(BsbBrightnessInfo(value))
    }
}
