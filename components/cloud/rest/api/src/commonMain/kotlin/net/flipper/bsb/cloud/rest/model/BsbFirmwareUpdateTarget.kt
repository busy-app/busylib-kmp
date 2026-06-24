package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class BsbFirmwareUpdateTarget(@Transient val target: Int) {
    @SerialName("f21")
    F21(target = 21),

    @SerialName("f22")
    F22(target = 22)
}
