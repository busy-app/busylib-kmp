package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrawRequest(
    @SerialName("app_id")
    val appId: String,
    val elements: List<Element>
) {
    @Serializable
    enum class Display {
        @SerialName("front")
        FRONT,

        @SerialName("back")
        BACK
    }

    @Serializable
    data class Element(
        val id: String,
        val timeout: Int,
        val type: ElementType,
        val text: String? = null,
        val path: String? = null,
        val x: Int,
        val y: Int,
        val display: Display
    ) {
        @Serializable
        enum class ElementType {
            @SerialName("image")
            IMAGE,

            @SerialName("text")
            TEXT
        }
    }
}
