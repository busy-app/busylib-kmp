package net.flipper.bridge.connection.screens.device.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import busylibkmp.sample.cmp.generated.resources.Res
import busylibkmp.sample.cmp.generated.resources.ic_more
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.BUSY_BAR_SCREEN_ASPECT_RATIO
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.rememberBusyImagePainter
import net.flipper.bridge.connection.screens.device.viewmodel.DevicesDropdownState
import net.flipper.tools.multistream.api.MultiStreamApi
import net.flipper.tools.multistream.api.MultiStreamState
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Suppress("LongMethod")
fun FDeviceDropdownComposable(
    devicesState: DevicesDropdownState,
    onDeviceSelect: (BUSYBar) -> Unit,
    onOpenSearch: () -> Unit,
    multiStreamApi: MultiStreamApi,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .border(1.dp, Color.Gray),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Text(
                text = devicesState.currentDevice
                    ?.humanReadableName
                    ?: "unknown",
                color = MaterialTheme.colors.onBackground
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                devicesState.devices.forEach { device ->
                    DropdownMenuItem(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDeviceSelect(device)
                            expanded = false
                        }
                    ) {
                        val frame by remember(device) {
                            multiStreamApi.get(device)
                                .filterIsInstance<MultiStreamState.Frame>()
                                .map { it.image }
                        }.collectAsState(null)

                        val painter = rememberBusyImagePainter(frame)
                        painter?.let {
                            Image(
                                modifier = Modifier.height(48.dp)
                                    .aspectRatio(BUSY_BAR_SCREEN_ASPECT_RATIO),
                                painter = it,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth
                            )
                        }
                        Text(
                            text = device.humanReadableName,
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                }

                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        expanded = false
                        onOpenSearch()
                    }
                ) {
                    Text(
                        text = "Click to add device",
                        color = MaterialTheme.colors.onBackground
                    )
                }
            }
        }

        Icon(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(Res.drawable.ic_more),
            contentDescription = null,
            tint = MaterialTheme.colors.onBackground
        )
    }
}
