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
    invalidateRpcInfo: () -> Unit,
    onOpenFM: () -> Unit,
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
            onClick = invalidateRpcInfo
        ) {
            Text(
                text = "Get device info",
                color = MaterialTheme.colors.onBackground
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenFM
        ) {
            Text(
                text = "File Manager",
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}
