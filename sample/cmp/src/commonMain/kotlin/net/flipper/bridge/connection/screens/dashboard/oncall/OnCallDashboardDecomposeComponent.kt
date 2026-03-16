package net.flipper.bridge.connection.screens.dashboard.oncall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class OnCallDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> OnCallDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        viewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        OnCallDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            onStartOnCall = viewModel::startOnCall,
            onStopOnCall = viewModel::stopOnCall
        )
    }
}
