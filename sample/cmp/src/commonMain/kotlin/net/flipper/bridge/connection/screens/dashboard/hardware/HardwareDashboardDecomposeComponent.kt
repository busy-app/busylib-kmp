package net.flipper.bridge.connection.screens.dashboard.hardware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class HardwareDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> HardwareDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        viewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val aboutDevice by viewModel.aboutDeviceFlow.collectAsState()

        HardwareDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            aboutDevice = aboutDevice
        )
    }
}
