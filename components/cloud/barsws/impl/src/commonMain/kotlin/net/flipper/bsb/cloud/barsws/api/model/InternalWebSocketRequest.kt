package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.barsws.api.WebSocketRequest

@Serializable
sealed interface InternalWebSocketRequest {
    @Serializable
    data class Authorization(
        @SerialName("token")
        val token: String
    ) : InternalWebSocketRequest

    @Serializable
    data class SubscriptionState(
        @SerialName("subscribe")
        val idsToSubscribe: List<String>? = null,
        @SerialName("unsubscribe")
        val idsToUnsubscribe: List<String>? = null
    ) : InternalWebSocketRequest
}

fun WebSocketRequest.toInternal() = when (this) {
    is WebSocketRequest.Subscribe -> InternalWebSocketRequest.SubscriptionState(
        idsToSubscribe = listOf(deviceId)
    )

    is WebSocketRequest.Unsubscribe -> InternalWebSocketRequest.SubscriptionState(
        idsToUnsubscribe = listOf(deviceId)
    )
}
