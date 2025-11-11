package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyBarStatus(
    @SerialName("system") val system: BusyBarStatusSystem,
    @SerialName("power") val power: BusyBarStatusPower
)

@Serializable
data class BusyBarStatusSystem(
    @SerialName("branch") val branch: String,
    @SerialName("version") val version: String,
    @SerialName("build_date") val buildDate: String,
    @SerialName("commit_hash") val commitHash: String,
    @SerialName("uptime") val uptime: String
)

@Serializable
data class BusyBarStatusPower(
    @SerialName("state") val state: PowerState,
    @SerialName("battery_charge") val batteryCharge: Int,
    @SerialName("battery_voltage") val batteryVoltage: Int,
    @SerialName("battery_current") val batteryCurrent: Int,
    @SerialName("usb_voltage") val usbVoltage: Int
)

@Serializable
enum class PowerState {
    @SerialName("discharging")
    DISCHARGING,

    @SerialName("charging")
    CHARGING,

    @SerialName("charged")
    CHARGED
}
