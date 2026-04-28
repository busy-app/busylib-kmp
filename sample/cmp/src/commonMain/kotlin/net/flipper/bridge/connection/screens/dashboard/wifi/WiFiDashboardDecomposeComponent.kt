package net.flipper.bridge.connection.screens.dashboard.wifi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class WiFiDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> WiFiDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate { viewModelFactory() }

    @Composable
    override fun Render(modifier: Modifier) {
        val status by viewModel.statusFlow.collectAsState()
        val networks by viewModel.networksFlow.collectAsState()
        val editingAllowed by viewModel.editingAllowedFlow.collectAsState()
        val state by viewModel.state.collectAsState()
        val actionState by viewModel.actionState.collectAsState()
        WiFiDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            status = status,
            networks = networks,
            editingAllowed = editingAllowed,
            state = state,
            actionState = actionState,
            onConnectToOpen = viewModel::connectToFirstOpenNetwork,
            onDisconnect = viewModel::disconnect
        )
    }
}
