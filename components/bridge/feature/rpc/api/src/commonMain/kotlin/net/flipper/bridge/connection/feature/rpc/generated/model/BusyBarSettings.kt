package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyBarSettings(
    @SerialName("theme")
    val theme: kotlin.String,
    @SerialName("show_work_phase_only")
    val showWorkPhaseOnly: kotlin.Boolean,
    @SerialName("trigger_smart_home")
    val triggerSmartHome: kotlin.Boolean
)
