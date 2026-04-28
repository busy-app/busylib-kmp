package net.flipper.bridge.connection.screens.dashboard.hub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class HubDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val onOpenSettings: () -> Unit,
    private val onOpenDeviceInfo: () -> Unit,
    private val onOpenAccount: () -> Unit,
    private val onOpenHardware: () -> Unit,
    private val onOpenOnCall: () -> Unit,
    private val onOpenSmartHome: () -> Unit,
    private val onOpenTimezone: () -> Unit,
    private val onOpenAssets: () -> Unit,
    private val onOpenDisplay: () -> Unit,
    private val onOpenScreenStreaming: () -> Unit,
    private val onOpenWiFi: () -> Unit,
    private val onOpenFwUpdate: () -> Unit
) : ScreenDecomposeComponent(componentContext) {
    @Composable
    override fun Render(modifier: Modifier) {
        HubDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            onOpenSettings = onOpenSettings,
            onOpenDeviceInfo = onOpenDeviceInfo,
            onOpenAccount = onOpenAccount,
            onOpenHardware = onOpenHardware,
            onOpenOnCall = onOpenOnCall,
            onOpenSmartHome = onOpenSmartHome,
            onOpenTimezone = onOpenTimezone,
            onOpenAssets = onOpenAssets,
            onOpenDisplay = onOpenDisplay,
            onOpenScreenStreaming = onOpenScreenStreaming,
            onOpenWiFi = onOpenWiFi,
            onOpenFwUpdate = onOpenFwUpdate
        )
    }
}
