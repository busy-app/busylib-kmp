package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BsbFirmwareUpdateTarget {
    @SerialName("f21")
    F21,

    @SerialName("f22")
    F22
}