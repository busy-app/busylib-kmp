package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetUpdateChangelogResponse(
    @SerialName("changelog")
    val changelog: String
)
