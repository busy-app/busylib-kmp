package net.flipper.bridge.connection.screens.device

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent
import net.flipper.bridge.connection.screens.device.composable.FCurrentDeviceComposable
import net.flipper.bridge.connection.screens.device.composable.FDeviceDropdownComposable
import net.flipper.bridge.connection.screens.device.composable.FPingComposable
import net.flipper.bridge.connection.screens.device.viewmodel.FCurrentDeviceViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.FDevicesViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.PingViewModel
import net.flipper.bridge.connection.screens.models.ConnectionRootConfig

class ConnectionDeviceScreenDecomposeComponent(
    componentContext: ComponentContext,
    private val navigation: StackNavigator<ConnectionRootConfig>,
    private val devicesViewModelProvider: () -> FDevicesViewModel,
    private val currentDeviceViewModelProvider: () -> FCurrentDeviceViewModel,
    private val pingViewModelProvider: () -> PingViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val devicesViewModel = instanceKeeper.getOrCreate {
        devicesViewModelProvider.invoke()
    }
    private val currentDeviceViewModel = instanceKeeper.getOrCreate {
        currentDeviceViewModelProvider.invoke()
    }
    private val pingViewModel = instanceKeeper.getOrCreate {
        pingViewModelProvider.invoke()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        Column {
            val deviceState by devicesViewModel.getState().collectAsState()
            FDeviceDropdownComposable(
                devicesState = deviceState,
                onDeviceSelect = devicesViewModel::onSelectDevice,
                onOpenSearch = { navigation.pushNew(ConnectionRootConfig.Search) },
                onDisconnect = devicesViewModel::onDisconnect
            )
            val currentDevice by currentDeviceViewModel.getState().collectAsState()
            FCurrentDeviceComposable(currentDevice)
            val logs by pingViewModel.getLogLinesState().collectAsState()
            FPingComposable(
                logs = logs,
                onSendPing = pingViewModel::sendPing,
                onDisconnect = currentDeviceViewModel::disconnect,
                onForget = currentDeviceViewModel::forget,
                onConnect = currentDeviceViewModel::connect
            )
        }
    }

    class Factory(
        private val devicesViewModelProvider: () -> FDevicesViewModel,
        private val currentDeviceViewModelProvider: () -> FCurrentDeviceViewModel,
        private val pingViewModelProvider: () -> PingViewModel
    ) {
        operator fun invoke(
            componentContext: ComponentContext,
            navigation: StackNavigator<ConnectionRootConfig>
        ): ConnectionDeviceScreenDecomposeComponent {
            return ConnectionDeviceScreenDecomposeComponent(
                componentContext = componentContext,
                navigation = navigation,
                devicesViewModelProvider = devicesViewModelProvider,
                currentDeviceViewModelProvider = currentDeviceViewModelProvider,
                pingViewModelProvider = pingViewModelProvider
            )
        }
    }
}
