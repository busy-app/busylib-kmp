package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.barsws.api.BUSYBarWebSocket
import kotlin.uuid.Uuid

@Serializable
data class BUSYBarWebSocketInternal(
    @SerialName("id")
    val id: Uuid,
    @SerialName("hardware_id")
    val hardwareId: String,
    @SerialName("name")
    val name: String? = null
)
fun BUSYBarWebSocketInternal.toPublic() = BUSYBarWebSocket(
    cloudId = id,
    hardwareId = hardwareId,
    name = name
)
