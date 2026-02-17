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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

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
                .safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val deviceName by viewModel.deviceNameFlow.collectAsState()
            Text("Device Name: $deviceName")
            val brightness by viewModel.brightnessFlow.collectAsState()
            Text("Brightness: $brightness")
            val volume by viewModel.volumeFlow.collectAsState()
            Text("Brightness: $volume")


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

            ScreenStreamingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Green)
            )
        }
    }

    @Composable
    private fun ScreenStreamingBlock(modifier: Modifier) {
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
        ): DashboardDecomposeComponent {
            return DashboardDecomposeComponent(
                componentContext = componentContext,
                dashboardViewModelFactory = dashboardViewModelFactory
            )
        }
    }
}
