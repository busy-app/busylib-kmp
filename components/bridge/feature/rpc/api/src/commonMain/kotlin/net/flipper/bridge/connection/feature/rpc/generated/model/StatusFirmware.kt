package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusFirmware(
    @SerialName("version")
    val version: kotlin.String,
    @SerialName("target")
    val target: kotlin.Int,
    @SerialName("branch")
    val branch: kotlin.String,
    @SerialName("build_date")
    val buildDate: kotlin.String,
    @SerialName("commit_hash")
    val commitHash: kotlin.String,
    @SerialName("nwp_version")
    val nwpVersion: kotlin.String? = null,
    @SerialName("matter_version")
    val matterVersion: kotlin.String? = null
)
