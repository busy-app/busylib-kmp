package net.flipper.bridge.connection.screens.device.composable

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList

@Composable
fun FPingComposable(
    logs: PersistentList<String>,
    onSendPing: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    onConnect: () -> Unit,
    toDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.Black)
        ) {
            items(logs) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = it,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSendPing
        ) {
            Text(
                text = "Send ping",
                color = MaterialTheme.colors.onBackground
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDisconnect
        ) {
            Text(
                text = "Disconnect",
                color = MaterialTheme.colors.onBackground
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConnect
        ) {
            Text(
                text = "Connect",
                color = MaterialTheme.colors.onBackground
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onForget
        ) {
            Text(
                text = "Forget",
                color = MaterialTheme.colors.onBackground
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = toDashboard
        ) {
            Text(
                text = "To Dashboard",
                color = MaterialTheme.colors.onBackground
            )
        }

    }
}
