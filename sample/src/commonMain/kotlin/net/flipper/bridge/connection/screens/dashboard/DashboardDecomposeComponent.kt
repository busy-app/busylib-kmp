package net.flipper.bridge.connection.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent
import net.flipper.bridge.connection.screens.device.ConnectionDeviceScreenDecomposeComponent
import net.flipper.bridge.connection.screens.device.viewmodel.FCurrentDeviceViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.FDevicesViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.PingViewModel
import net.flipper.bridge.connection.screens.models.ConnectionRootConfig

class DashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val dashboardViewModelFactory: () -> DashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        dashboardViewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        Column(
            modifier.fillMaxSize()
                .safeDrawingPadding()
        ) {
            val deviceName by viewModel.getDeviceName().collectAsState()
            Text("Device Name: $deviceName")
        }
    }

    class Factory(
        private val dashboardViewModelFactory: () -> DashboardViewModel
    ) {
        operator fun invoke(
            componentContext: ComponentContext,
        ): DashboardDecomposeComponent {
            return DashboardDecomposeComponent(
                componentContext = componentContext,
                dashboardViewModelFactory = dashboardViewModelFactory
            )
        }
    }
}