package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.barsws.api.WebSocketRequest
import kotlin.uuid.Uuid

@Serializable
sealed interface InternalWebSocketRequest {
    @Serializable
    data class Authorization(
        @SerialName("token")
        val token: String
    ) : InternalWebSocketRequest

    @Serializable
    data class SubscribeState(
        @SerialName("subscribe")
        val idsToSubscribe: List<Uuid>,
    ) : InternalWebSocketRequest

    @Serializable
    data class UnsubscribeState(
        @SerialName("unsubscribe")
        val idsToUnsubscribe: List<Uuid>
    ) : InternalWebSocketRequest
}

fun WebSocketRequest.toInternal() = when (this) {
    is WebSocketRequest.Subscribe -> InternalWebSocketRequest.SubscribeState(
        idsToSubscribe = listOf(deviceId)
    )

    is WebSocketRequest.Unsubscribe -> InternalWebSocketRequest.UnsubscribeState(
        idsToUnsubscribe = listOf(deviceId)
    )
}
