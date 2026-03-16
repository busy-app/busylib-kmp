package net.flipper.bridge.connection.screens.dashboard

import kotlinx.serialization.Serializable

@Serializable
sealed class DashboardConfig {
    @Serializable
    data object Hub : DashboardConfig()

    @Serializable
    data object Overview : DashboardConfig()

    @Serializable
    data object DeviceInfo : DashboardConfig()

    @Serializable
    data object Account : DashboardConfig()

    @Serializable
    data object Hardware : DashboardConfig()

    @Serializable
    data object OnCall : DashboardConfig()

    @Serializable
    data object ScreenStreaming : DashboardConfig()
}
