package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StorageListResponse(
    @SerialName("list")
    val list: List<Entry>
) {
    @Serializable
    data class Entry(
        @SerialName("type")
        val type: Type,
        @SerialName("name")
        val name: String,
        @SerialName("size")
        val size: Long? = null
    ) {
        @Serializable
        enum class Type {
            @SerialName("file")
            FILE,

            @SerialName("dir")
            DIR
        }
    }
}
