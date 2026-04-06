package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrawRequest(
    @SerialName("application_name")
    val appId: String,
    val priority: Int? = null,
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
        val timeout: Int? = null,
        @SerialName("display_until")
        val displayUntil: String? = null,
        val display: Display = Display.FRONT,
        val type: ElementType,
        val text: String? = null,
        val path: String? = null,
        @SerialName("stock_path")
        val stockPath: String? = null,
        val x: Int? = null,
        val y: Int? = null,
        val align: Alignment? = null,
        val font: Font? = null,
        val color: String? = null,
        val width: Int? = null,
        @SerialName("scroll_rate")
        val scrollRate: Int? = null,
        val section: String? = null,
        val loop: Boolean? = null,
        @SerialName("await_previous_end")
        val awaitPreviousEnd: Boolean? = null,
        val timestamp: String? = null,
        val direction: CountdownDirection? = null,
        @SerialName("show_hours")
        val showHours: ShowHours? = null
    ) {
        @Serializable
        enum class ElementType {
            @SerialName("image")
            IMAGE,

            @SerialName("text")
            TEXT,

            @SerialName("animation")
            ANIMATION,

            @SerialName("countdown")
            COUNTDOWN
        }

        @Serializable
        enum class Alignment {
            @SerialName("top_left")
            TOP_LEFT,

            @SerialName("top_mid")
            TOP_MID,

            @SerialName("top_right")
            TOP_RIGHT,

            @SerialName("mid_left")
            MID_LEFT,

            @SerialName("center")
            CENTER,

            @SerialName("mid_right")
            MID_RIGHT,

            @SerialName("bottom_left")
            BOTTOM_LEFT,

            @SerialName("bottom_mid")
            BOTTOM_MID,

            @SerialName("bottom_right")
            BOTTOM_RIGHT
        }

        @Serializable
        enum class Font {
            @SerialName("small")
            SMALL,

            @SerialName("medium")
            MEDIUM,

            @SerialName("medium_condensed")
            MEDIUM_CONDENSED,

            @SerialName("big")
            BIG
        }

        @Serializable
        enum class CountdownDirection {
            @SerialName("time_left")
            TIME_LEFT,

            @SerialName("time_since")
            TIME_SINCE
        }

        @Serializable
        enum class ShowHours {
            @SerialName("when_non_zero")
            WHEN_NON_ZERO,

            @SerialName("always")
            ALWAYS
        }
    }
}
