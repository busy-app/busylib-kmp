package net.flipper.bridge.connection.screens.dashboard.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun DashboardButtonRow(
    primaryTitle: String,
    onPrimaryClick: () -> Unit,
    secondaryTitle: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(primaryTitle)
        }
        OutlinedButton(
            onClick = onSecondaryClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(secondaryTitle)
        }
    }
}

@Composable
fun DashboardLogCard(
    state: DashboardActionState,
    modifier: Modifier = Modifier
) {
    DashboardSectionCard(
        title = "Log",
        modifier = modifier
    ) {
        Text("Last action: ${state.lastAction.orUnavailable()}")
        if (state.logs.isEmpty()) {
            Text("No actions yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.logs.forEach { line ->
                    Text(line)
                }
            }
        }
    }
}
