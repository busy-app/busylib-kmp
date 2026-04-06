package net.flipper.bridge.connection.screens.dashboard.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class DisplayDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    viewModelFactory: () -> DisplayDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = viewModelFactory()

    @Composable
    override fun Render(modifier: Modifier) {
        val state by viewModel.state.collectAsState()
        val actionState by viewModel.actionState.collectAsState()
        DisplayDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            state = state,
            actionState = actionState,
            onDrawSampleText = viewModel::drawSampleText,
            onDrawSampleAnimation = viewModel::drawSampleAnimation,
            onClearSampleDraw = viewModel::clearSampleDraw
        )
    }
}
