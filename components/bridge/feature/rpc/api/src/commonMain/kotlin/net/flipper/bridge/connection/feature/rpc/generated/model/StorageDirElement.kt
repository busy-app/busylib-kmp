package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StorageDirElement(
    @SerialName("type")
    val type: Type,
    @SerialName("name")
    val name: kotlin.String
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("file")
        FILE("file"),

        @SerialName("dir")
        DIR("dir")
    }
}
