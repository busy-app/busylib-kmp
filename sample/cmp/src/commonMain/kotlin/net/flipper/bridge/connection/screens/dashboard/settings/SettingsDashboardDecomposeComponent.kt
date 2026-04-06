package net.flipper.bridge.connection.screens.dashboard.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class SettingsDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> SettingsDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        viewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val state by viewModel.state.collectAsState()
        val actionState by viewModel.actionState.collectAsState()
        val deviceName by viewModel.deviceNameFlow.collectAsState()
        val brightness by viewModel.brightnessFlow.collectAsState()
        val volume by viewModel.volumeFlow.collectAsState()

        SettingsDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            state = state,
            actionState = actionState,
            deviceName = deviceName,
            brightness = brightness?.value,
            volume = volume?.volume,
            onSetDeviceName = viewModel::setDeviceName,
            onSetBrightness = viewModel::setBrightness,
            onSetBrightnessAuto = viewModel::setBrightnessAuto,
            onSetVolume = viewModel::setVolume
        )
    }
}
