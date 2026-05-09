package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.core.busylib.data.serialization.DurationSerializer
import net.flipper.core.busylib.data.serialization.InstantUtcSerializer
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class StatusSystem(
    @SerialName("api_semver")
    val apiSemver: kotlin.String,
    @SerialName("uptime")
    @Serializable(DurationSerializer::class)
    val uptime: Duration,
    @SerialName("boot_time")
    @Serializable(InstantUtcSerializer::class)
    val bootTime: Instant,
    @SerialName("auto_update_enabled")
    val autoUpdateEnabled: kotlin.Boolean
)
