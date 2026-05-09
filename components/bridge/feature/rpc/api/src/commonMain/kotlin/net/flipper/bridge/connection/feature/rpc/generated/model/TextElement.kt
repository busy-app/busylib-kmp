package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TextElement(
    @SerialName("id")
    val id: kotlin.String,
    @SerialName("type")
    val type: Type,
    @SerialName("text")
    val text: kotlin.String,
    @SerialName("font")
    val font: Font,
    @SerialName("timeout")
    val timeout: kotlin.Int? = null,
    @SerialName("display_until")
    val displayUntil: kotlin.String? = null,
    @SerialName("x")
    val x: kotlin.Int? = null,
    @SerialName("y")
    val y: kotlin.Int? = null,
    @SerialName("display")
    val display: Display? = null,
    @SerialName("align")
    val align: Align? = null,
    @SerialName("color")
    val color: kotlin.String? = null,
    @SerialName("width")
    val width: kotlin.Int? = null,
    @SerialName("scroll_rate")
    val scrollRate: kotlin.Int? = null
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("text")
        TEXT("text"),

        @SerialName("image")
        IMAGE("image"),

        @SerialName("animation")
        ANIMATION("animation"),

        @SerialName("countdown")
        COUNTDOWN("countdown")
    }

    @Serializable
    enum class Font(val value: kotlin.String) {
        @SerialName("small")
        SMALL("small"),

        @SerialName("medium")
        MEDIUM("medium"),

        @SerialName("medium_condensed")
        MEDIUM_CONDENSED("medium_condensed"),

        @SerialName("big")
        BIG("big")
    }

    @Serializable
    enum class Display(val value: kotlin.String) {
        @SerialName("front")
        FRONT("front"),

        @SerialName("back")
        BACK("back")
    }

    @Serializable
    enum class Align(val value: kotlin.String) {
        @SerialName("top_left")
        TOP_LEFT("top_left"),

        @SerialName("top_mid")
        TOP_MID("top_mid"),

        @SerialName("top_right")
        TOP_RIGHT("top_right"),

        @SerialName("mid_left")
        MID_LEFT("mid_left"),

        @SerialName("center")
        CENTER("center"),

        @SerialName("mid_right")
        MID_RIGHT("mid_right"),

        @SerialName("bottom_left")
        BOTTOM_LEFT("bottom_left"),

        @SerialName("bottom_mid")
        BOTTOM_MID("bottom_mid"),

        @SerialName("bottom_right")
        BOTTOM_RIGHT("bottom_right")
    }
}
