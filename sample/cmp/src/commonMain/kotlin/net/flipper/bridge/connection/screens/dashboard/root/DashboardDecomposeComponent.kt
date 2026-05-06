package net.flipper.bridge.connection.screens.dashboard.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import net.flipper.bridge.connection.screens.dashboard.account.AccountDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.account.AccountDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.assets.AssetsDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.assets.AssetsDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.deviceinfo.DeviceInfoDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.deviceinfo.DeviceInfoDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.display.DisplayDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.display.DisplayDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.hardware.HardwareDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.hardware.HardwareDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.hub.HubDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.oncall.OnCallDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.oncall.OnCallDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.root.model.DashboardConfig
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.ScreenStreamingDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.ScreenStreamingDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.settings.SettingsDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.settings.SettingsDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.smarthome.SmartHomeDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.smarthome.SmartHomeDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.timezone.TimezoneDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.timezone.TimezoneDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.wifi.WiFiDashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.wifi.WiFiDashboardViewModel
import net.flipper.bridge.connection.screens.decompose.CompositeDecomposeComponent
import net.flipper.bridge.connection.screens.decompose.DecomposeComponent
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.fwupdate.FirmwareUpdateDecomposeComponent
import net.flipper.bridge.connection.screens.fwupdate.FirmwareUpdateViewModel
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi

class DashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    private val settingsViewModelFactory: () -> SettingsDashboardViewModel,
    private val deviceInfoViewModelFactory: () -> DeviceInfoDashboardViewModel,
    private val accountViewModelFactory: () -> AccountDashboardViewModel,
    private val hardwareViewModelFactory: () -> HardwareDashboardViewModel,
    private val onCallViewModelFactory: () -> OnCallDashboardViewModel,
    private val smartHomeViewModelFactory: () -> SmartHomeDashboardViewModel,
    private val timezoneViewModelFactory: () -> TimezoneDashboardViewModel,
    private val assetsViewModelFactory: () -> AssetsDashboardViewModel,
    private val displayViewModelFactory: () -> DisplayDashboardViewModel,
    private val screenStreamingViewModelFactory: () -> ScreenStreamingDashboardViewModel,
    private val wifiViewModelFactory: () -> WiFiDashboardViewModel,
    private val firmwareUpdateViewModelFactory: () -> FirmwareUpdateViewModel,
) : CompositeDecomposeComponent<DashboardConfig>(), ComponentContext by componentContext {
    override val stack: Value<ChildStack<DashboardConfig, DecomposeComponent>> = childStack(
        source = navigation,
        serializer = DashboardConfig.serializer(),
        initialConfiguration = DashboardConfig.Hub,
        childFactory = ::child,
        handleBackButton = true
    )

    @Suppress("LongMethod")
    private fun child(
        config: DashboardConfig,
        componentContext: ComponentContext
    ): DecomposeComponent = when (config) {
        DashboardConfig.Hub -> createHubComponent(componentContext)

        DashboardConfig.Settings -> SettingsDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = settingsViewModelFactory
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

        DashboardConfig.SmartHome -> SmartHomeDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = smartHomeViewModelFactory
        )

        DashboardConfig.Timezone -> TimezoneDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = timezoneViewModelFactory
        )

        DashboardConfig.Assets -> AssetsDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = assetsViewModelFactory
        )

        DashboardConfig.Display -> DisplayDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = displayViewModelFactory
        )

        DashboardConfig.ScreenStreaming -> ScreenStreamingDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = screenStreamingViewModelFactory
        )

        DashboardConfig.WiFi -> WiFiDashboardDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            viewModelFactory = wifiViewModelFactory
        )

        DashboardConfig.FirmwareUpdate -> FirmwareUpdateDecomposeComponent(
            componentContext = componentContext,
            onBack = navigation::pop,
            firmwareUpdateViewModelFactory = firmwareUpdateViewModelFactory,
            busyFirmwareDirectoryChannelApi = busyFirmwareDirectoryChannelApi
        )
    }

    private fun createHubComponent(componentContext: ComponentContext) = HubDashboardDecomposeComponent(
        componentContext = componentContext,
        onBack = onBack,
        onOpenSettings = { navigation.pushNew(DashboardConfig.Settings) },
        onOpenDeviceInfo = { navigation.pushNew(DashboardConfig.DeviceInfo) },
        onOpenAccount = { navigation.pushNew(DashboardConfig.Account) },
        onOpenHardware = { navigation.pushNew(DashboardConfig.Hardware) },
        onOpenOnCall = { navigation.pushNew(DashboardConfig.OnCall) },
        onOpenSmartHome = { navigation.pushNew(DashboardConfig.SmartHome) },
        onOpenTimezone = { navigation.pushNew(DashboardConfig.Timezone) },
        onOpenAssets = { navigation.pushNew(DashboardConfig.Assets) },
        onOpenDisplay = { navigation.pushNew(DashboardConfig.Display) },
        onOpenScreenStreaming = { navigation.pushNew(DashboardConfig.ScreenStreaming) },
        onOpenWiFi = { navigation.pushNew(DashboardConfig.WiFi) },
        onOpenFwUpdate = { navigation.pushNew(DashboardConfig.FirmwareUpdate) }
    )

    class Factory(
        private val settingsViewModelFactory: () -> SettingsDashboardViewModel,
        private val deviceInfoViewModelFactory: () -> DeviceInfoDashboardViewModel,
        private val accountViewModelFactory: () -> AccountDashboardViewModel,
        private val hardwareViewModelFactory: () -> HardwareDashboardViewModel,
        private val onCallViewModelFactory: () -> OnCallDashboardViewModel,
        private val smartHomeViewModelFactory: () -> SmartHomeDashboardViewModel,
        private val timezoneViewModelFactory: () -> TimezoneDashboardViewModel,
        private val assetsViewModelFactory: () -> AssetsDashboardViewModel,
        private val displayViewModelFactory: () -> DisplayDashboardViewModel,
        private val screenStreamingViewModelFactory: () -> ScreenStreamingDashboardViewModel,
        private val wifiViewModelFactory: () -> WiFiDashboardViewModel,
        private val firmwareUpdateViewModelFactory: () -> FirmwareUpdateViewModel,
        private val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    ) {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: DecomposeOnBackParameter
        ): DashboardDecomposeComponent {
            return DashboardDecomposeComponent(
                componentContext = componentContext,
                onBack = onBack,
                settingsViewModelFactory = settingsViewModelFactory,
                deviceInfoViewModelFactory = deviceInfoViewModelFactory,
                accountViewModelFactory = accountViewModelFactory,
                hardwareViewModelFactory = hardwareViewModelFactory,
                onCallViewModelFactory = onCallViewModelFactory,
                smartHomeViewModelFactory = smartHomeViewModelFactory,
                timezoneViewModelFactory = timezoneViewModelFactory,
                assetsViewModelFactory = assetsViewModelFactory,
                displayViewModelFactory = displayViewModelFactory,
                screenStreamingViewModelFactory = screenStreamingViewModelFactory,
                wifiViewModelFactory = wifiViewModelFactory,
                firmwareUpdateViewModelFactory = firmwareUpdateViewModelFactory,
                busyFirmwareDirectoryChannelApi = busyFirmwareDirectoryChannelApi
            )
        }
    }
}
