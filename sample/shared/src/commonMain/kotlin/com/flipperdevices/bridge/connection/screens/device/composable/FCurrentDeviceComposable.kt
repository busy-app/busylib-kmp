package com.flipperdevices.bridge.connection.screens.device.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flipperdevices.bridge.connection.orchestrator.api.model.FDeviceConnectStatus

@Composable
fun FCurrentDeviceComposable(
    deviceConnectStatus: FDeviceConnectStatus,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "State: ${deviceConnectStatus.getHumanReadableName()}",
            color = MaterialTheme.colors.onBackground
        )
        Text(
            text = "Name: ${deviceConnectStatus.currentDevice()?.humanReadableName}",
            color = MaterialTheme.colors.onBackground
        )
    }
}

fun FDeviceConnectStatus.currentDevice() = when (this) {
    is FDeviceConnectStatus.Connected -> device
    is FDeviceConnectStatus.Connecting -> device
    is FDeviceConnectStatus.Disconnected -> device
    is FDeviceConnectStatus.Disconnecting -> device
}

fun FDeviceConnectStatus.getHumanReadableName() = when (this) {
    is FDeviceConnectStatus.Connected -> "Connected"
    is FDeviceConnectStatus.Connecting -> "Connecting"
    is FDeviceConnectStatus.Disconnected -> "Disconnected"
    is FDeviceConnectStatus.Disconnecting -> "Disconnecting"
}
