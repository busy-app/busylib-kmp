package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountProfile(
    @SerialName("profile")
    val profile: Profile,
    @SerialName("custom_url")
    val customUrl: kotlin.String? = null
) {

    @Serializable
    enum class Profile(val value: kotlin.String) {
        @SerialName("dev")
        DEV("dev"),

        @SerialName("prod")
        PROD("prod"),

        @SerialName("local")
        LOCAL("local"),

        @SerialName("custom")
        CUSTOM("custom")
    }
}
