package net.flipper.bridge.connection.screens.dashboard.screenstreaming

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class ScreenStreamingDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> ScreenStreamingDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        viewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val streamImage by viewModel.screenStreamingImagesFlow.collectAsState(null)

        ScreenStreamingDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            streamImage = streamImage
        )
    }
}
