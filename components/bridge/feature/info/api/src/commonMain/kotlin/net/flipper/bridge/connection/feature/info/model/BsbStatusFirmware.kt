package net.flipper.bridge.connection.feature.info.model

data class BsbStatusFirmware(
    val version: String,
    val target: Int,
    val branch: String,
    val buildDate: String,
    val commitHash: String,
    val nwpVersion: String?,
    val matterVersion: String?
)
