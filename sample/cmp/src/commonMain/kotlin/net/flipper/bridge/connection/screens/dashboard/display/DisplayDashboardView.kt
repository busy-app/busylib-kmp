package net.flipper.bridge.connection.screens.dashboard.display

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardButtonRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardLogCard
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun DisplayDashboardContent(
    onBack: () -> Unit,
    state: DisplayDashboardState,
    actionState: DashboardActionState,
    onDrawSampleText: () -> Unit,
    onDrawSampleAnimation: () -> Unit,
    onClearSampleDraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Display",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Draw",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Last draw result: ${state.lastDrawSummary.orUnavailable()}")
            DashboardButtonRow(
                primaryTitle = "Draw Text",
                onPrimaryClick = onDrawSampleText,
                secondaryTitle = "Draw Animation",
                onSecondaryClick = onDrawSampleAnimation
            )
            OutlinedButton(
                onClick = onClearSampleDraw,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Draw")
            }
        }

        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
