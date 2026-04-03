package net.flipper.bridge.connection.feature.link.check.onready.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ForceUnlinkedAccountInfo(
    @SerialName("force_unlinked_user_ids")
    val forceUnlinkedUserIds: Set<Uuid> = emptySet()
)
