package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Status(
    @SerialName("device")
    val device: StatusDevice,
    @SerialName("firmware")
    val firmware: StatusFirmware,
    @SerialName("system")
    val system: StatusSystem,
    @SerialName("power")
    val power: StatusPower
)
