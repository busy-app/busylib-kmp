package net.flipper.bridge.connection.screens.dashboard.assets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class AssetsDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    viewModelFactory: () -> AssetsDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = viewModelFactory()

    @Composable
    override fun Render(modifier: Modifier) {
        val state by viewModel.state.collectAsState()
        val actionState by viewModel.actionState.collectAsState()
        AssetsDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            state = state,
            actionState = actionState,
            onUploadSampleAsset = viewModel::uploadSampleAsset
        )
    }
}
