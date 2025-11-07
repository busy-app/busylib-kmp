package com.flipperdevices.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class RpcLinkedAccountInfo(
    @SerialName("state")
    val state: State,
    @SerialName("email")
    val email: String? = null,
    @SerialName("id")
    val id: String? = null
) {
    val uuid: Uuid? = id?.let { Uuid.parse(it) }

    @Serializable
    enum class State {
        @SerialName("not_linked")
        NOT_LINKED,

        @SerialName("error")
        ERROR,

        @SerialName("linked")
        LINKED,

        @SerialName("disconnected")
        DISCONNECTED,
    }
}
