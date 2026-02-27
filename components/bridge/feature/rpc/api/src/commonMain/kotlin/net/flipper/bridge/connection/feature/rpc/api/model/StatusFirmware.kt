package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusFirmware(
    @SerialName("version")
    val version: String,
    @SerialName("target")
    val target: Int,
    @SerialName("branch")
    val branch: String,
    @SerialName("build_date")
    val buildDate: String,
    @SerialName("commit_hash")
    val commitHash: String,
    @SerialName("nwp_version")
    val nwpVersion: String? = null
)
