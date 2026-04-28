package net.flipper.bridge.connection.screens.dashboard.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardButtonRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardLogCard
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun WiFiDashboardContent(
    onBack: () -> Unit,
    status: BsbWifiStatusResponse?,
    networks: ImmutableList<WiFiNetwork>?,
    editingAllowed: Boolean?,
    state: WiFiDashboardState,
    actionState: DashboardActionState,
    onConnectToOpen: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "WiFi",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Status",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "State", value = status?.state?.name.orUnavailable())
            DashboardInfoRow(label = "SSID", value = status?.ssid.orUnavailable())
            DashboardInfoRow(label = "RSSI", value = status?.rssi?.toString().orUnavailable())
            DashboardInfoRow(label = "Channel", value = status?.channel?.toString().orUnavailable())
            DashboardInfoRow(label = "IP", value = status?.ipConfig?.address.orUnavailable())
            DashboardInfoRow(label = "Editing allowed", value = editingAllowed?.toString().orUnavailable())
        }

        DashboardSectionCard(
            title = "Actions",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Last action: ${state.lastConnectSummary.orUnavailable()}")
            DashboardButtonRow(
                primaryTitle = "Connect Open",
                onPrimaryClick = onConnectToOpen,
                secondaryTitle = "Disconnect",
                onSecondaryClick = onDisconnect
            )
        }

        DashboardSectionCard(
            title = "Networks (${networks?.size ?: 0})",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val list = networks
            if (list.isNullOrEmpty()) {
                Text("No networks discovered")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    list.forEach { network ->
                        DashboardInfoRow(
                            label = "${network.ssid} (${network.wifiSecurity.label()})",
                            value = "${network.rssi} dBm"
                        )
                    }
                }
            }
        }

        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun WiFiSecurity.label(): String = when (this) {
    WiFiSecurity.Supported.None -> "Open"
    is WiFiSecurity.Supported.Password -> name
    is WiFiSecurity.Other -> "Other(${internalWifiSecurity.name})"
}
