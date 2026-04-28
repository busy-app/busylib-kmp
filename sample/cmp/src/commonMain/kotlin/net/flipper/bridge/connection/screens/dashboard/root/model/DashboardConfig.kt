package net.flipper.bridge.connection.screens.dashboard.root.model

import kotlinx.serialization.Serializable

@Serializable
sealed class DashboardConfig {
    @Serializable
    data object Hub : DashboardConfig()

    @Serializable
    data object Settings : DashboardConfig()

    @Serializable
    data object DeviceInfo : DashboardConfig()

    @Serializable
    data object Account : DashboardConfig()

    @Serializable
    data object Hardware : DashboardConfig()

    @Serializable
    data object OnCall : DashboardConfig()

    @Serializable
    data object SmartHome : DashboardConfig()

    @Serializable
    data object Timezone : DashboardConfig()

    @Serializable
    data object Assets : DashboardConfig()

    @Serializable
    data object Display : DashboardConfig()

    @Serializable
    data object ScreenStreaming : DashboardConfig()

    @Serializable
    data object WiFi : DashboardConfig()

    @Serializable
    data object FirmwareUpdate : DashboardConfig()
}
