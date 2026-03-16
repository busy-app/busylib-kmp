package net.flipper.bridge.connection.screens.dashboard.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.dashboard.orUnavailable
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class OverviewDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> OverviewDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        viewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val deviceName by viewModel.deviceNameFlow.collectAsState()
        val brightness by viewModel.brightnessFlow.collectAsState()
        val volume by viewModel.volumeFlow.collectAsState()
        val deviceVersion by viewModel.deviceVersionFlow.collectAsState()

        OverviewDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            deviceName = deviceName.orUnavailable(),
            brightness = brightness?.value.orUnavailable(),
            volume = volume?.volume.orUnavailable(),
            deviceVersion = deviceVersion?.version.orUnavailable()
        )
    }
}
