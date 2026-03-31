package net.flipper.bridge.connection.screens.dashboard.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import net.flipper.bridge.connection.screens.dashboard.account.AccountDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.account.AccountDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.deviceinfo.DeviceInfoDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.deviceinfo.DeviceInfoDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.hardware.HardwareDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.hardware.HardwareDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.hub.HubDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.oncall.OnCallDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.oncall.OnCallDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.overview.OverviewDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.overview.OverviewDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.root.model.DashboardConfig
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.ScreenStreamingDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.ScreenStreamingDashboardViewModel
import net.flipper.bridge.connection.screens.decompose.CompositeDecomposeComponent
import net.flipper.bridge.connection.screens.decompose.DecomposeComponent
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.fwupdate.FirmwareUpdateDecomposeComponent
import net.flipper.bridge.connection.screens.fwupdate.FirmwareUpdateViewModel

class DashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val overviewViewModelFactory: () -> OverviewDashboardViewModel,
    private val deviceInfoViewModelFactory: () -> DeviceInfoDashboardViewModel,
    private val accountViewModelFactory: () -> AccountDashboardViewModel,
    private val hardwareViewModelFactory: () -> HardwareDashboardViewModel,
    private val onCallViewModelFactory: () -> OnCallDashboardViewModel,
    private val screenStreamingViewModelFactory: () -> ScreenStreamingDashboardViewModel,
    private val firmwareUpdateViewModelFactory: () -> FirmwareUpdateViewModel,
) : CompositeDecomposeComponent<DashboardConfig>(), ComponentContext by componentContext {
    override val stack: Value<ChildStack<DashboardConfig, DecomposeComponent>> = childStack(
        source = navigation,
        serializer = DashboardConfig.serializer(),
        initialConfiguration = DashboardConfig.Hub,
        childFactory = ::child,
        handleBackButton = true
    )

    private fun child(
        config: DashboardConfig,
        componentContext: ComponentContext
    ): DecomposeComponent = when (config) {
        DashboardConfig.Hub -> HubDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = onBack,
            onOpenOverview = { navigation.pushNew(DashboardConfig.Overview) },
            onOpenDeviceInfo = { navigation.pushNew(DashboardConfig.DeviceInfo) },
            onOpenAccount = { navigation.pushNew(DashboardConfig.Account) },
            onOpenHardware = { navigation.pushNew(DashboardConfig.Hardware) },
            onOpenOnCall = { navigation.pushNew(DashboardConfig.OnCall) },
            onOpenScreenStreaming = { navigation.pushNew(DashboardConfig.ScreenStreaming) },
            onOpenFwUpdate = { navigation.pushNew(DashboardConfig.FirmwareUpdate) }
        )

        DashboardConfig.Overview -> OverviewDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = overviewViewModelFactory
        )

        DashboardConfig.DeviceInfo -> DeviceInfoDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = deviceInfoViewModelFactory
        )

        DashboardConfig.Account -> AccountDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = accountViewModelFactory
        )

        DashboardConfig.Hardware -> HardwareDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = hardwareViewModelFactory
        )

        DashboardConfig.OnCall -> OnCallDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = onCallViewModelFactory
        )

        DashboardConfig.ScreenStreaming -> ScreenStreamingDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = screenStreamingViewModelFactory
        )

        DashboardConfig.FirmwareUpdate -> FirmwareUpdateDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            firmwareUpdateViewModelFactory = firmwareUpdateViewModelFactory
        )
    }

    class Factory(
        private val overviewViewModelFactory: () -> OverviewDashboardViewModel,
        private val deviceInfoViewModelFactory: () -> DeviceInfoDashboardViewModel,
        private val accountViewModelFactory: () -> AccountDashboardViewModel,
        private val hardwareViewModelFactory: () -> HardwareDashboardViewModel,
        private val onCallViewModelFactory: () -> OnCallDashboardViewModel,
        private val screenStreamingViewModelFactory: () -> ScreenStreamingDashboardViewModel,
        private val firmwareUpdateViewModelFactory: () -> FirmwareUpdateViewModel,
    ) {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: DecomposeOnBackParameter
        ): DashboardDecomposeComponent {
            return DashboardDecomposeComponent(
                componentContext = componentContext,
                onBack = onBack,
                overviewViewModelFactory = overviewViewModelFactory,
                deviceInfoViewModelFactory = deviceInfoViewModelFactory,
                accountViewModelFactory = accountViewModelFactory,
                hardwareViewModelFactory = hardwareViewModelFactory,
                onCallViewModelFactory = onCallViewModelFactory,
                screenStreamingViewModelFactory = screenStreamingViewModelFactory,
                firmwareUpdateViewModelFactory = firmwareUpdateViewModelFactory
            )
        }
    }
}
