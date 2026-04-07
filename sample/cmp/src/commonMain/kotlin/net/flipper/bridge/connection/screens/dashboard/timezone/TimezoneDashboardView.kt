package net.flipper.bridge.connection.screens.dashboard.timezone

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardButtonRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardLogCard
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun TimezoneDashboardContent(
    onBack: () -> Unit,
    timezoneInfo: String?,
    state: TimezoneDashboardState,
    actionState: DashboardActionState,
    onRefreshTimezones: () -> Unit,
    onSetCurrentOrFirstTimezone: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Timezone",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Timezone",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "Current timezone", value = timezoneInfo.orUnavailable())
            Text("Last timezone list: ${state.lastTimezonesSummary.orUnavailable()}")
            Text("Last set timezone: ${state.lastSetTimezone.orUnavailable()}")
            DashboardButtonRow(
                primaryTitle = "Refresh List",
                onPrimaryClick = onRefreshTimezones,
                secondaryTitle = "Set Active/First",
                onSecondaryClick = onSetCurrentOrFirstTimezone
            )
        }

        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
