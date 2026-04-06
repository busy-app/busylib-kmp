package net.flipper.bridge.connection.screens.dashboard.timezone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class TimezoneDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    viewModelFactory: () -> TimezoneDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = viewModelFactory()

    @Composable
    override fun Render(modifier: Modifier) {
        val timezoneInfo by viewModel.timezoneInfoFlow.collectAsState()
        val timestampInfo by viewModel.timestampInfoFlow.collectAsState()
        val state by viewModel.state.collectAsState()
        val actionState by viewModel.actionState.collectAsState()
        TimezoneDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            timezoneInfo = timezoneInfo?.name,
            timestampInfo = timestampInfo?.timestamp.toString(),
            state = state,
            actionState = actionState,
            onRefreshTimezones = viewModel::refreshTimezones,
            onSetCurrentOrFirstTimezone = viewModel::setCurrentOrFirstTimezone
        )
    }
}
