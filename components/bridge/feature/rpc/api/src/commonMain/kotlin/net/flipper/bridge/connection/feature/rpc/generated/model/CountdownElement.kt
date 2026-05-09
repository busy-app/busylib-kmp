package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountdownElement(
    @SerialName("id")
    val id: kotlin.String,
    @SerialName("type")
    val type: Type,
    @SerialName("timestamp")
    val timestamp: kotlin.String,
    @SerialName("direction")
    val direction: Direction,
    @SerialName("show_hours")
    val showHours: ShowHours,
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
    val color: kotlin.String? = null
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
    enum class Direction(val value: kotlin.String) {
        @SerialName("time_left")
        TIME_LEFT("time_left"),

        @SerialName("time_since")
        TIME_SINCE("time_since")
    }

    @Serializable
    enum class ShowHours(val value: kotlin.String) {
        @SerialName("when_non_zero")
        WHEN_NON_ZERO("when_non_zero"),

        @SerialName("always")
        ALWAYS("always")
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
