package net.flipper.bridge.connection.screens.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import busylibkmp.sample.cmp.generated.resources.Res
import busylibkmp.sample.cmp.generated.resources.material_ic_refresh
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
import net.flipper.bridge.connection.screens.root.model.ConnectionRootConfig
import net.flipper.tools.multistream.api.MultiStreamApi
import org.jetbrains.compose.resources.painterResource

class ConnectionDeviceScreenDecomposeComponent(
    componentContext: ComponentContext,
    private val navigation: StackNavigator<ConnectionRootConfig>,
    private val devicesViewModelProvider: () -> FDevicesViewModel,
    private val currentDeviceViewModelProvider: () -> FCurrentDeviceViewModel,
    private val pingViewModelProvider: () -> PingViewModel,
    private val multiStreamApi: MultiStreamApi
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FDeviceDropdownComposable(
                    devicesState = deviceState,
                    onDeviceSelect = devicesViewModel::onSelectDevice,
                    onOpenSearch = { navigation.pushNew(ConnectionRootConfig.Search) },
                    multiStreamApi = multiStreamApi,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = currentDeviceViewModel::refresh) {
                    Icon(
                        painter = painterResource(Res.drawable.material_ic_refresh),
                        contentDescription = "Refresh connection",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            val currentDevice by currentDeviceViewModel.getState().collectAsState()
            FCurrentDeviceComposable(currentDevice)
            val logs by pingViewModel.getLogLinesState().collectAsState()
            FPingComposable(
                logs = logs,
                onSendPing = pingViewModel::sendPing,
                onForget = currentDeviceViewModel::forget,
                toDashboard = {
                    navigation.pushNew(ConnectionRootConfig.Dashboard)
                },
            )
        }
    }

    class Factory(
        private val devicesViewModelProvider: () -> FDevicesViewModel,
        private val currentDeviceViewModelProvider: () -> FCurrentDeviceViewModel,
        private val pingViewModelProvider: () -> PingViewModel,
        private val multiStreamApi: MultiStreamApi,
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
                pingViewModelProvider = pingViewModelProvider,
                multiStreamApi = multiStreamApi,
            )
        }
    }
}
