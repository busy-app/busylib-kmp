package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface InternalWebSocketRequest {
    @Serializable
    data class Authorization(
        @SerialName("token")
        val token: String
    ) : InternalWebSocketRequest {
        /**
         * We don't want [token] in logs
         */
        override fun toString(): String {
            return "Authorization(token=***)"
        }
    }

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
