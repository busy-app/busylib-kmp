package net.flipper.bridge.connection.screens.dashboard.oncall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard

@Composable
fun OnCallDashboardContent(
    onBack: () -> Unit,
    onStartOnCall: () -> Unit,
    onStopOnCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "On-Call",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "On-Call",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartOnCall,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enable")
                }
                OutlinedButton(
                    onClick = onStopOnCall,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disable")
                }
            }
        }
    }
}
