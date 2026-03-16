package net.flipper.bridge.connection.screens.dashboard.deviceinfo

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun DeviceInfoDashboardContent(
    onBack: () -> Unit,
    deviceInfo: BusyBarStatusSystem?,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Device Info",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Device Info",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "API semver", value = deviceInfo?.apiSemver.orUnavailable())
            DashboardInfoRow(label = "Uptime", value = deviceInfo?.uptime.orUnavailable())
            DashboardInfoRow(label = "Boot time", value = deviceInfo?.bootTime.orUnavailable())
        }
    }
}
