package net.flipper.bridge.connection.screens.dashboard.hardware

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.about.model.BusyBarAboutDevice
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.orUnavailable

@Composable
fun HardwareDashboardContent(
    onBack: () -> Unit,
    aboutDevice: BusyBarAboutDevice?,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Hardware",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Hardware",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "Serial number", value = aboutDevice?.serialNumber.orUnavailable())
            DashboardInfoRow(label = "BLE MAC", value = aboutDevice?.macAddressBluetooth.orUnavailable())
            DashboardInfoRow(label = "Wi-Fi MAC", value = aboutDevice?.macAddressWifi.orUnavailable())
            DashboardInfoRow(label = "USB MAC", value = aboutDevice?.macAddressUsb.orUnavailable())
            DashboardInfoRow(label = "Hardware version", value = aboutDevice?.hardwareVersion.orUnavailable())
            DashboardInfoRow(label = "Production date", value = aboutDevice?.productionDate.orUnavailable())
            DashboardInfoRow(
                label = "Front display",
                value = aboutDevice?.frontDisplayResolution.orUnavailable()
            )
            DashboardInfoRow(
                label = "Front refresh rate",
                value = aboutDevice?.frontDisplayRefreshRate.orUnavailable()
            )
            DashboardInfoRow(label = "Back display", value = aboutDevice?.backDisplayResolution.orUnavailable())
            DashboardInfoRow(label = "Central MCU", value = aboutDevice?.centralMcu.orUnavailable())
            DashboardInfoRow(label = "RAM size", value = aboutDevice?.ramSize.orUnavailable())
        }
    }
}
