package net.flipper.bridge.connection.screens.dashboard.smarthome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class SmartHomeDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> SmartHomeDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate { viewModelFactory() }

    @Composable
    override fun Render(modifier: Modifier) {
        val commissionedFabrics by viewModel.commissionedFabricsFlow.collectAsState()
        val pairCodeWithTimeLeft by viewModel.pairCodeWithTimeLeftFlow.collectAsState()
        val state by viewModel.state.collectAsState()
        val actionState by viewModel.actionState.collectAsState()
        SmartHomeDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            commissionedFabrics = commissionedFabrics,
            pairCodeWithTimeLeft = pairCodeWithTimeLeft,
            state = state,
            actionState = actionState,
            onRequestPairCode = viewModel::requestPairCode,
            onForgetAllPairings = viewModel::forgetAllPairings
        )
    }
}
