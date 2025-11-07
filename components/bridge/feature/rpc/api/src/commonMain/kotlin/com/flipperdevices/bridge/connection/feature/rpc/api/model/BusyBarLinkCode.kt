package com.flipperdevices.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyBarLinkCode(
    @SerialName("code")
    val code: String
)