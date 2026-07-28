package net.flipper.bridge.connection.screens.dashboard.drawtool

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
fun DrawToolDashboardContent(
    onBack: () -> Unit,
    state: DrawToolDashboardState,
    actionState: DashboardActionState,
    onShowPreviewOnFront: () -> Unit,
    onShowPreviewOnBack: () -> Unit,
    onHidePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Draw Tool",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Preview",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Last preview result: ${state.lastPreviewSummary.orUnavailable()}")
            DashboardButtonRow(
                primaryTitle = "Show on Front",
                onPrimaryClick = onShowPreviewOnFront,
                secondaryTitle = "Show on Back",
                onSecondaryClick = onShowPreviewOnBack
            )
            OutlinedButton(
                onClick = onHidePreview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hide Preview")
            }
        }

        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
