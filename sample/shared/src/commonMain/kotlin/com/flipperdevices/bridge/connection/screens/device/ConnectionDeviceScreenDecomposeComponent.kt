package com.flipperdevices.bridge.connection.screens.device

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.flipperdevices.bridge.connection.screens.decompose.ScreenDecomposeComponent
import com.flipperdevices.bridge.connection.screens.device.composable.FCurrentDeviceComposable
import com.flipperdevices.bridge.connection.screens.device.composable.FDeviceDropdownComposable
import com.flipperdevices.bridge.connection.screens.device.composable.FPingComposable
import com.flipperdevices.bridge.connection.screens.device.viewmodel.FCurrentDeviceViewModel
import com.flipperdevices.bridge.connection.screens.device.viewmodel.FDevicesViewModel
import com.flipperdevices.bridge.connection.screens.device.viewmodel.PingViewModel
import com.flipperdevices.bridge.connection.screens.models.ConnectionRootConfig
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider

@Inject
class ConnectionDeviceScreenDecomposeComponent(
    @Assisted componentContext: ComponentContext,
    @Assisted private val navigation: StackNavigator<ConnectionRootConfig>,
    private val devicesViewModelProvider: Provider<FDevicesViewModel>,
    private val currentDeviceViewModelProvider: Provider<FCurrentDeviceViewModel>,
    private val pingViewModelProvider: Provider<PingViewModel>
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
                invalidateRpcInfo = { },
                onOpenFM = {
                    navigation.pushNew(ConnectionRootConfig.FileManager)
                }
            )
        }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            @Assisted componentContext: ComponentContext,
            @Assisted navigation: StackNavigator<ConnectionRootConfig>
        ): ConnectionDeviceScreenDecomposeComponent
    }
}
