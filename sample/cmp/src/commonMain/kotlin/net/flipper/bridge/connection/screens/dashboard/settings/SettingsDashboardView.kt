package net.flipper.bridge.connection.screens.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.settings.model.BsbBrightness
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardLogCard
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable
import net.flipper.core.busylib.data.Fraction
import kotlin.math.roundToInt

@Composable
fun SettingsDashboardContent(
    onBack: () -> Unit,
    state: SettingsDashboardState,
    actionState: DashboardActionState,
    deviceName: String?,
    brightness: BsbBrightness?,
    volume: Fraction?,
    onSetDeviceName: (String) -> Unit,
    onSetBrightness: (Int) -> Unit,
    onSetBrightnessAuto: () -> Unit,
    onSetVolume: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVolumePercent = volume?.toWholePercent()?.roundToInt()
    val currentBrightnessPercent = (brightness as? BsbBrightness.Number)
        ?.value
        ?.toWholePercent()
        ?.roundToInt()

    DashboardScreenLayout(
        modifier = modifier,
        title = "Settings",
        onBack = onBack
    ) {
        SettingsCurrentValuesSection(deviceName, brightness, volume)
        SettingsDeviceNameSection(deviceName, state, onSetDeviceName)
        SettingsBrightnessSection(currentBrightnessPercent, state, onSetBrightness, onSetBrightnessAuto)
        SettingsVolumeSection(currentVolumePercent, state, onSetVolume)
        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SettingsCurrentValuesSection(
    deviceName: String?,
    brightness: BsbBrightness?,
    volume: Fraction?
) {
    DashboardSectionCard(
        title = "Current Values",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        DashboardInfoRow(label = "Device name", value = deviceName.orUnavailable())
        DashboardInfoRow(label = "Brightness", value = brightness.toUiText())
        DashboardInfoRow(label = "Volume", value = volume.toUiText())
    }
}

@Composable
private fun SettingsDeviceNameSection(
    deviceName: String?,
    state: SettingsDashboardState,
    onSetDeviceName: (String) -> Unit
) {
    DashboardSectionCard(
        title = "Device Name",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        var deviceNameDraft by rememberSaveable(deviceName) {
            mutableStateOf(deviceName.orEmpty())
        }

        OutlinedTextField(
            value = deviceNameDraft,
            onValueChange = { deviceNameDraft = it },
            label = { Text("Device name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onSetDeviceName(deviceNameDraft) },
            enabled = deviceNameDraft.isNotBlank() && !state.isUpdatingDeviceName,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isUpdatingDeviceName) "Saving..." else "Save Device Name")
        }
    }
}

@Composable
private fun SettingsBrightnessSection(
    currentBrightnessPercent: Int?,
    state: SettingsDashboardState,
    onSetBrightness: (Int) -> Unit,
    onSetBrightnessAuto: () -> Unit
) {
    DashboardSectionCard(
        title = "Brightness",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        var brightnessDraft by rememberSaveable(currentBrightnessPercent) {
            mutableStateOf(currentBrightnessPercent?.toString().orEmpty())
        }
        val brightnessPercent = brightnessDraft.parsePercent()

        OutlinedTextField(
            value = brightnessDraft,
            onValueChange = { brightnessDraft = it },
            label = { Text("Brightness %") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (brightnessDraft.isNotBlank() && brightnessPercent == null) {
            Text(
                text = "Enter a whole number from 0 to 100",
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { brightnessPercent?.let(onSetBrightness) },
                enabled = brightnessPercent != null && !state.isUpdatingBrightness,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.isUpdatingBrightness) "Saving..." else "Set Manual")
            }
            OutlinedButton(
                onClick = onSetBrightnessAuto,
                enabled = !state.isUpdatingBrightness,
                modifier = Modifier.weight(1f)
            ) {
                Text("Set Auto")
            }
        }
    }
}

@Composable
private fun SettingsVolumeSection(
    currentVolumePercent: Int?,
    state: SettingsDashboardState,
    onSetVolume: (Int) -> Unit
) {
    DashboardSectionCard(
        title = "Volume",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        var volumeDraft by rememberSaveable(currentVolumePercent) {
            mutableStateOf(currentVolumePercent?.toString().orEmpty())
        }
        val volumePercent = volumeDraft.parsePercent()

        OutlinedTextField(
            value = volumeDraft,
            onValueChange = { volumeDraft = it },
            label = { Text("Volume %") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (volumeDraft.isNotBlank() && volumePercent == null) {
            Text(
                text = "Enter a whole number from 0 to 100",
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption
            )
        }
        Button(
            onClick = { volumePercent?.let(onSetVolume) },
            enabled = volumePercent != null && !state.isUpdatingVolume,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isUpdatingVolume) "Saving..." else "Save Volume")
        }
    }
}

private const val MAX_PERCENT = 100

private fun String.parsePercent(): Int? {
    return trim().toIntOrNull()?.takeIf { it in 0..MAX_PERCENT }
}

private fun BsbBrightness?.toUiText(): String {
    return when (this) {
        null -> "Unavailable"
        BsbBrightness.Auto -> "Auto"
        is BsbBrightness.Number -> "${value.toWholePercent().roundToInt()}%"
    }
}

private fun Fraction?.toUiText(): String {
    return this?.toWholePercent()?.roundToInt()?.let { "$it%" } ?: "Unavailable"
}
