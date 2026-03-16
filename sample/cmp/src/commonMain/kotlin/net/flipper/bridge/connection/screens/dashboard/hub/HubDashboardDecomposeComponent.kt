package net.flipper.bridge.connection.screens.dashboard.hub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class HubDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val onOpenOverview: () -> Unit,
    private val onOpenDeviceInfo: () -> Unit,
    private val onOpenAccount: () -> Unit,
    private val onOpenHardware: () -> Unit,
    private val onOpenOnCall: () -> Unit,
    private val onOpenScreenStreaming: () -> Unit
) : ScreenDecomposeComponent(componentContext) {
    @Composable
    override fun Render(modifier: Modifier) {
        HubDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            onOpenOverview = onOpenOverview,
            onOpenDeviceInfo = onOpenDeviceInfo,
            onOpenAccount = onOpenAccount,
            onOpenHardware = onOpenHardware,
            onOpenOnCall = onOpenOnCall,
            onOpenScreenStreaming = onOpenScreenStreaming
        )
    }
}
