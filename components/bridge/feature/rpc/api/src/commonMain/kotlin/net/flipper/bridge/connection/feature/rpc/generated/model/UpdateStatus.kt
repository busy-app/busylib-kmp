package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStatus(
    @SerialName("install")
    val install: UpdateStatusInstall,
    @SerialName("check")
    val check: UpdateStatusCheck
)
