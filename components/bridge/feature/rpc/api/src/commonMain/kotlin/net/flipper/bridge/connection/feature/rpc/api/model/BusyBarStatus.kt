package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.rpc.api.serialization.InstantUtcSerializer
import kotlin.time.Instant

@Serializable
data class BusyBarStatus(
    @SerialName("device") val device: BusyBarStatusDevice,
    @SerialName("firmware") val firmware: StatusFirmware,
    @SerialName("system") val system: BusyBarStatusSystem,
    @SerialName("power") val power: BusyBarStatusPower
)

@Serializable
data class BusyBarStatusDevice(
    @SerialName("serial_number") val serialNumber: String,
    @SerialName("usb_mac") val usbMac: String,
    @SerialName("wifi_mac") val wifiMac: String,
    @SerialName("ble_mac") val bleMac: String,
    @SerialName("otp_valid") val otpValid: Boolean
)

@Serializable
data class BusyBarStatusSystem(
    @SerialName("api_semver")
    val apiSemver: String,
    @SerialName("uptime")
    val uptime: String,
    @SerialName("boot_time")
    @Serializable(InstantUtcSerializer::class)
    val bootTime: Instant
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
