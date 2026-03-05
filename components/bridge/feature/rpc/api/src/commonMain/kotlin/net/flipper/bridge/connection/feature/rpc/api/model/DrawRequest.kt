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
        val priority: Int? = null,
        val display: Display,
        val type: ElementType,
        val text: String? = null,
        val path: String? = null,
        val x: Int? = null,
        val y: Int? = null,
        @SerialName("builtin_anim")
        val builtinAnim: String? = null,
        val section: String? = null,
        val loop: Boolean? = null
    ) {
        @Serializable
        enum class ElementType {
            @SerialName("image")
            IMAGE,

            @SerialName("text")
            TEXT,

            @SerialName("anim")
            ANIM
        }
    }
}
