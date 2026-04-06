package net.flipper.bridge.connection.screens.dashboard.assets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardLogCard
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun AssetsDashboardContent(
    onBack: () -> Unit,
    state: AssetsDashboardState,
    actionState: DashboardActionState,
    onUploadSampleAsset: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Assets",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Upload",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Last asset upload: ${state.lastUploadedAssetPath.orUnavailable()}")
            Button(
                onClick = onUploadSampleAsset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload Asset")
            }
        }

        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
