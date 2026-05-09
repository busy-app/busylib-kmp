package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisplayElements(
    @SerialName("application_name")
    val applicationName: kotlin.String,
    @SerialName("elements")
    val elements: kotlin.collections.List<DisplayElementsElementsInner>,
    @SerialName("priority")
    val priority: kotlin.Int? = null
)
