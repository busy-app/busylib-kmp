package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusDevice(
    @SerialName("serial_number")
    val serialNumber: kotlin.String,
    @SerialName("usb_mac")
    val usbMac: kotlin.String,
    @SerialName("otp_valid")
    val otpValid: kotlin.Boolean,
    @SerialName("wifi_mac")
    val wifiMac: kotlin.String? = null,
    @SerialName("ble_mac")
    val bleMac: kotlin.String? = null,
    @SerialName("otp_model")
    val otpModel: kotlin.String? = null,
    @SerialName("otp_timestamp")
    val otpTimestamp: kotlin.Int? = null
)
