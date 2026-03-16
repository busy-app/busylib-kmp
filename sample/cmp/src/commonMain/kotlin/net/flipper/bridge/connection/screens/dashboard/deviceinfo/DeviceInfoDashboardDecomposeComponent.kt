package net.flipper.bridge.connection.screens.dashboard.deviceinfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class DeviceInfoDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> DeviceInfoDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        viewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val deviceInfo by viewModel.deviceInfoFlow.collectAsState()

        DeviceInfoDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            deviceInfo = deviceInfo?.getOrNull()
        )
    }
}
