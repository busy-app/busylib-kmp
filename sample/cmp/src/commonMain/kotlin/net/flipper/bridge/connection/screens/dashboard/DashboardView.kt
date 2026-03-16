package net.flipper.bridge.connection.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.about.model.BusyBarAboutDevice
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat

@Composable
fun DashboardContent(
    onBack: () -> Unit,
    deviceName: String,
    brightness: String,
    volume: String,
    deviceInfo: BusyBarStatusSystem?,
    deviceVersion: String,
    linkedAccountStatus: LinkedAccountInfo?,
    aboutDevice: BusyBarAboutDevice?,
    streamImage: BusyImageFormat?,
    onDeleteLinkedAccount: () -> Unit,
    onStartOnCall: () -> Unit,
    modifier: Modifier = Modifier,
    onStopOnCall: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardTopBar(onBack = onBack)
        OverviewSection(
            deviceName = deviceName,
            brightness = brightness,
            volume = volume,
            deviceVersion = deviceVersion
        )
        DeviceInfoSection(
            deviceInfo = deviceInfo,
        )
        AccountSection(
            linkedAccountStatus = linkedAccountStatus,
            onDeleteLinkedAccount = onDeleteLinkedAccount
        )
        HardwareSection(aboutDevice = aboutDevice)
        OnCallSection(
            onStartOnCall = onStartOnCall,
            onStopOnCall = onStopOnCall
        )
        ScreenStreamingSection(streamImage = streamImage)
    }
}

@Composable
private fun DashboardTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Dashboard") },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp,
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    )
}

@Composable
private fun OverviewSection(
    deviceName: String,
    brightness: String,
    volume: String,
    deviceVersion: String
) {
    SectionCard(
        title = "Overview",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        InfoRow(label = "Device name", value = deviceName)
        InfoRow(label = "Brightness", value = brightness)
        InfoRow(label = "Volume", value = volume)
        InfoRow(label = "Version", value = deviceVersion)
    }
}

@Composable
private fun DeviceInfoSection(
    deviceInfo: BusyBarStatusSystem?,
) {
    SectionCard(
        title = "Device Info",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        InfoRow(label = "API semver", value = deviceInfo?.apiSemver.orUnavailable())
        InfoRow(label = "Uptime", value = deviceInfo?.uptime.orUnavailable())
        InfoRow(label = "Boot time", value = deviceInfo?.bootTime.orUnavailable())
    }
}

@Composable
private fun AccountSection(
    linkedAccountStatus: LinkedAccountInfo?,
    onDeleteLinkedAccount: () -> Unit
) {
    SectionCard(
        title = "Account",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        InfoRow(label = "Linked account", value = linkedAccountStatus.toUiText())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onDeleteLinkedAccount,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MaterialTheme.colors.surface
            )
        ) {
            Text("Delete linked account")
        }
    }
}

@Composable
private fun HardwareSection(aboutDevice: BusyBarAboutDevice?) {
    SectionCard(
        title = "Hardware",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        InfoRow(label = "Serial number", value = aboutDevice?.serialNumber.orUnavailable())
        InfoRow(label = "BLE MAC", value = aboutDevice?.macAddressBluetooth.orUnavailable())
        InfoRow(label = "Wi-Fi MAC", value = aboutDevice?.macAddressWifi.orUnavailable())
        InfoRow(label = "USB MAC", value = aboutDevice?.macAddressUsb.orUnavailable())
        InfoRow(label = "Hardware version", value = aboutDevice?.hardwareVersion.orUnavailable())
        InfoRow(label = "Production date", value = aboutDevice?.productionDate.orUnavailable())
        InfoRow(
            label = "Front display",
            value = aboutDevice?.frontDisplayResolution.orUnavailable()
        )
        InfoRow(
            label = "Front refresh rate",
            value = aboutDevice?.frontDisplayRefreshRate.orUnavailable()
        )
        InfoRow(label = "Back display", value = aboutDevice?.backDisplayResolution.orUnavailable())
        InfoRow(label = "Central MCU", value = aboutDevice?.centralMcu.orUnavailable())
        InfoRow(label = "RAM size", value = aboutDevice?.ramSize.orUnavailable())
    }
}

@Composable
private fun OnCallSection(
    onStartOnCall: () -> Unit,
    onStopOnCall: () -> Unit
) {
    SectionCard(
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

@Composable
private fun ScreenStreamingSection(streamImage: BusyImageFormat?) {
    SectionCard(
        title = "Screen Streaming",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        ScreenStreamingBlock(
            modifier = Modifier.fillMaxWidth(),
            image = streamImage
        )
    }
}

@Composable
private fun ScreenStreamingBlock(
    image: BusyImageFormat?,
    modifier: Modifier = Modifier
) {
    val painter = rememberBusyImagePainter(image)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        if (painter != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Waiting for stream...",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
                )
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                content()
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            color = MaterialTheme.colors.onSurface
        )
    }
}
