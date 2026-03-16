package net.flipper.bridge.connection.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class DashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val dashboardViewModelFactory: () -> DashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        dashboardViewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val deviceName by viewModel.deviceNameFlow.collectAsState()
        val brightness by viewModel.brightnessFlow.collectAsState()
        val volume by viewModel.volumeFlow.collectAsState()
        val deviceInfo by viewModel.deviceInfoFlow.collectAsState()
        val deviceVersion by viewModel.deviceVersionFlow.collectAsState()
        val linkedAccountStatus by viewModel.linkedAccountStatusFlow.collectAsState()
        val aboutDevice by viewModel.aboutDeviceFlow.collectAsState()
        val streamImage by viewModel.screenStreamingImagesFlow.collectAsState(null)

        DashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            deviceName = deviceName.orUnavailable(),
            brightness = brightness?.value.orUnavailable(),
            volume = volume?.volume.orUnavailable(),
            deviceInfo = deviceInfo?.getOrNull(),
            deviceVersion = deviceVersion?.version.orUnavailable(),
            linkedAccountStatus = linkedAccountStatus,
            aboutDevice = aboutDevice,
            streamImage = streamImage,
            onDeleteLinkedAccount = viewModel::deleteAccountBsb,
            onStartOnCall = viewModel::startOnCall,
            onStopOnCall = viewModel::stopOnCall
        )
    }

    class Factory(
        private val dashboardViewModelFactory: () -> DashboardViewModel
    ) {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: DecomposeOnBackParameter
        ): DashboardDecomposeComponent {
            return DashboardDecomposeComponent(
                componentContext = componentContext,
                onBack = onBack,
                dashboardViewModelFactory = dashboardViewModelFactory
            )
        }
    }
}

fun <T> T?.orUnavailable(): String = this?.toString() ?: "Unavailable"
