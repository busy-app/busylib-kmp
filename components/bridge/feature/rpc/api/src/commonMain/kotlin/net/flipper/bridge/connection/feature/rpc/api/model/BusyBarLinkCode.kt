package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface BusyBarLinkCodeResponse

@Serializable
data class BusyBarLinkCode(
    @SerialName("code")
    val code: String
) : BusyBarLinkCodeResponse

@Serializable
data object BusyBarLinkCodeAlreadyLinked : BusyBarLinkCodeResponse
