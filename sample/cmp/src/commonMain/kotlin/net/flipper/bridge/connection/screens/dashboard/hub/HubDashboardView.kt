package net.flipper.bridge.connection.screens.dashboard.hub

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureButton
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard

@Composable
fun HubDashboardContent(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDeviceInfo: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenHardware: () -> Unit,
    onOpenOnCall: () -> Unit,
    onOpenSmartHome: () -> Unit,
    onOpenTimezone: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenScreenStreaming: () -> Unit,
    onOpenWiFi: () -> Unit,
    onOpenFwUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Dashboard",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Features",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardFeatureButton(title = "Settings", onClick = onOpenSettings)
            DashboardFeatureButton(title = "Device Info", onClick = onOpenDeviceInfo)
            DashboardFeatureButton(title = "Account", onClick = onOpenAccount)
            DashboardFeatureButton(title = "Hardware", onClick = onOpenHardware)
            DashboardFeatureButton(title = "On-Call", onClick = onOpenOnCall)
            DashboardFeatureButton(title = "Smart Home", onClick = onOpenSmartHome)
            DashboardFeatureButton(title = "Timezone", onClick = onOpenTimezone)
            DashboardFeatureButton(title = "Assets", onClick = onOpenAssets)
            DashboardFeatureButton(title = "Display", onClick = onOpenDisplay)
            DashboardFeatureButton(title = "Screen Streaming", onClick = onOpenScreenStreaming)
            DashboardFeatureButton(title = "WiFi", onClick = onOpenWiFi)
            DashboardFeatureButton(title = "Firmware Update", onClick = onOpenFwUpdate)
        }
    }
}
