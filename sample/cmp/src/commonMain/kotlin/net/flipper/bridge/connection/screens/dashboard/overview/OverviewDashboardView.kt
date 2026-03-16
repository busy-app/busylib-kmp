package net.flipper.bridge.connection.screens.dashboard.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard

@Composable
fun OverviewDashboardContent(
    onBack: () -> Unit,
    deviceName: String,
    brightness: String,
    volume: String,
    deviceVersion: String,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Overview",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Overview",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "Device name", value = deviceName)
            DashboardInfoRow(label = "Brightness", value = brightness)
            DashboardInfoRow(label = "Volume", value = volume)
            DashboardInfoRow(label = "Version", value = deviceVersion)
        }
    }
}
