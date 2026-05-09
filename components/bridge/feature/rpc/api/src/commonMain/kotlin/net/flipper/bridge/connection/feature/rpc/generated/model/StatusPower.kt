package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusPower(
    @SerialName("state")
    val state: State,
    @SerialName("battery_charge")
    val batteryCharge: kotlin.Int,
    @SerialName("battery_voltage")
    val batteryVoltage: kotlin.Int,
    @SerialName("battery_current")
    val batteryCurrent: kotlin.Int,
    @SerialName("usb_voltage")
    val usbVoltage: kotlin.Int
) {

    @Serializable
    enum class State(val value: kotlin.String) {
        @SerialName("discharging")
        DISCHARGING("discharging"),

        @SerialName("charging")
        CHARGING("charging"),

        @SerialName("charged")
        CHARGED("charged")
    }
}
