package net.flipper.bridge.connection.screens.dashboard.deviceinfo

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.info.model.BsbBusyBarStatusSystem
import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion
import net.flipper.bridge.connection.feature.info.model.BsbStatusFirmware
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun DeviceInfoDashboardContent(
    onBack: () -> Unit,
    deviceInfo: BsbBusyBarStatusSystem?,
    deviceFirmware: BsbStatusFirmware?,
    deviceVersion: BsbBusyBarVersion?,
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
            DashboardInfoRow(label = "Device version", value = deviceVersion?.version.orUnavailable())
            DashboardInfoRow(label = "API semver", value = deviceInfo?.apiSemver.orUnavailable())
            DashboardInfoRow(label = "Uptime", value = deviceInfo?.uptime.orUnavailable())
            DashboardInfoRow(label = "Boot time", value = deviceInfo?.bootTime.orUnavailable())
        }

        DashboardSectionCard(
            title = "Firmware",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "Version", value = deviceFirmware?.version.orUnavailable())
            DashboardInfoRow(label = "Target", value = deviceFirmware?.target.orUnavailable())
            DashboardInfoRow(label = "Branch", value = deviceFirmware?.branch.orUnavailable())
            DashboardInfoRow(label = "Build date", value = deviceFirmware?.buildDate.orUnavailable())
            DashboardInfoRow(label = "Commit hash", value = deviceFirmware?.commitHash.orUnavailable())
            DashboardInfoRow(label = "NWP version", value = deviceFirmware?.nwpVersion.orUnavailable())
        }
    }
}
