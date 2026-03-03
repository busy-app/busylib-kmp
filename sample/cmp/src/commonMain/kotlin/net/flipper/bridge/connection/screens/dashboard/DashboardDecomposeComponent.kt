package net.flipper.bridge.connection.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent
import net.flipper.bridge.connection.screens.models.ConnectionRootConfig

class DashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val navigation: StackNavigator<ConnectionRootConfig>,
    private val dashboardViewModelFactory: () -> DashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        dashboardViewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        Column(
            modifier.fillMaxSize()
                .safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val deviceName by viewModel.deviceNameFlow.collectAsState()
            Text("Device Name: $deviceName")
            val brightness by viewModel.brightnessFlow.collectAsState()
            Text("Brightness: $brightness")
            val volume by viewModel.volumeFlow.collectAsState()
            Text("Volume: $volume")
            val deviceInfo by viewModel.deviceInfoFlow.collectAsState()
            Text("Device Info: $deviceInfo")
            val deviceVersion by viewModel.deviceVersionFlow.collectAsState()
            Text("Device Version: $deviceVersion")

            Button(
                onClick = viewModel::startOnCall
            ) {
                Text("Enable on call")
            }

            Button(
                onClick = viewModel::stopOnCall
            ) {
                Text("Disable on call")
            }

            Button(
                onClick = { navigation.pushNew(ConnectionRootConfig.FirmwareUpdate) }
            ) {
                Text("Firmware Update")
            }

            ScreenStreamingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Green)
            )
        }
    }

    @Composable
    private fun ScreenStreamingBlock(modifier: Modifier = Modifier) {
        val image by viewModel.screenStreamingImagesFlow.collectAsState(null)
        val painter = rememberBusyImagePainter(image)
        painter?.let {
            Image(
                modifier = modifier,
                painter = it,
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        }
    }

    class Factory(
        private val dashboardViewModelFactory: () -> DashboardViewModel
    ) {
        operator fun invoke(
            componentContext: ComponentContext,
            navigation: StackNavigator<ConnectionRootConfig>
        ): DashboardDecomposeComponent {
            return DashboardDecomposeComponent(
                componentContext = componentContext,
                navigation = navigation,
                dashboardViewModelFactory = dashboardViewModelFactory
            )
        }
    }
}
