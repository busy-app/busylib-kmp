package net.flipper.bridge.connection.screens.di

import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.dashboard.account.AccountDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.assets.AssetsDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.deviceinfo.DeviceInfoDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.display.DisplayDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.hardware.HardwareDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.oncall.OnCallDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.root.DashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.ScreenStreamingDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.settings.SettingsDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.smarthome.SmartHomeDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.timezone.TimezoneDashboardViewModel
import net.flipper.bridge.connection.screens.device.ConnectionDeviceScreenDecomposeComponent
import net.flipper.bridge.connection.screens.device.viewmodel.FCurrentDeviceViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.FDevicesViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.PingViewModel
import net.flipper.bridge.connection.screens.fwupdate.FirmwareUpdateViewModel
import net.flipper.bridge.connection.screens.root.ConnectionRootDecomposeComponent
import net.flipper.bridge.connection.screens.search.ConnectionSearchDecomposeComponent
import net.flipper.bridge.connection.screens.search.ConnectionSearchViewModel
import net.flipper.bridge.connection.screens.utils.PermissionChecker
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiSampleImpl
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi
import net.flipper.busylib.BUSYLib
import net.flipper.tools.multistream.api.MultiStreamApi

fun getRootDecomposeComponent(
    componentContext: ComponentContext,
    permissionChecker: PermissionChecker,
    persistedStorage: FDevicePersistedStorage,
    busyLib: BUSYLib,
    searchViewModelProvider: () -> ConnectionSearchViewModel,
    principalApi: UserPrincipalApiSampleImpl
): ConnectionRootDecomposeComponent {
    return getRootDecomposeComponentFactory(
        permissionChecker = permissionChecker,
        persistedStorage = persistedStorage,
        orchestrator = busyLib.orchestrator,
        featureProvider = busyLib.featureProvider,
        searchViewModelProvider = searchViewModelProvider,
        fConnectionService = busyLib.connectionService,
        firmwareUpdaterApi = busyLib.firmwareUpdaterApi,
        principalApi = principalApi,
        multiStreamApi = busyLib.multiStreamApi
    ).invoke(componentContext)
}

@Suppress("LongParameterList")
private fun getRootDecomposeComponentFactory(
    permissionChecker: PermissionChecker,
    persistedStorage: FDevicePersistedStorage,
    orchestrator: FDeviceOrchestrator,
    featureProvider: FFeatureProvider,
    fConnectionService: FConnectionService,
    searchViewModelProvider: () -> ConnectionSearchViewModel,
    firmwareUpdaterApi: FirmwareUpdaterApi,
    principalApi: UserPrincipalApiSampleImpl,
    multiStreamApi: MultiStreamApi
): ConnectionRootDecomposeComponent.Factory {
    return ConnectionRootDecomposeComponent.Factory(
        permissionChecker = permissionChecker,
        searchDecomposeFactory = getSearchDecomposeFactory(
            searchViewModelProvider = searchViewModelProvider,
        ),
        connectionDeviceScreenDecomposeComponentFactory = getConnectionDeviceScreenDecomposeComponentFactory(
            persistedStorage = persistedStorage,
            orchestrator = orchestrator,
            featureProvider = featureProvider,
            fService = fConnectionService,
            multiStreamApi = multiStreamApi,
        ),
        dashboardDecomposeComponentFactory = getDashboardDecomposeComponentFactory(
            fFeatureProvider = featureProvider,
            principalApi = principalApi,
            firmwareUpdaterApi = firmwareUpdaterApi
        ),
    )
}

private fun getSearchDecomposeFactory(
    searchViewModelProvider: () -> ConnectionSearchViewModel,
): ConnectionSearchDecomposeComponent.Factory {
    return ConnectionSearchDecomposeComponent.Factory(
        searchViewModelProvider = searchViewModelProvider,
    )
}

private fun getConnectionDeviceScreenDecomposeComponentFactory(
    persistedStorage: FDevicePersistedStorage,
    orchestrator: FDeviceOrchestrator,
    featureProvider: FFeatureProvider,
    fService: FConnectionService,
    multiStreamApi: MultiStreamApi,
): ConnectionDeviceScreenDecomposeComponent.Factory {
    return ConnectionDeviceScreenDecomposeComponent.Factory(
        devicesViewModelProvider = { FDevicesViewModel(persistedStorage) },
        currentDeviceViewModelProvider = {
            FCurrentDeviceViewModel(
                orchestrator,
                fService,
                persistedStorage
            )
        },
        pingViewModelProvider = { PingViewModel(featureProvider, orchestrator) },
        multiStreamApi = multiStreamApi,
    )
}

private fun getDashboardDecomposeComponentFactory(
    fFeatureProvider: FFeatureProvider,
    principalApi: UserPrincipalApiSampleImpl,
    firmwareUpdaterApi: FirmwareUpdaterApi
): DashboardDecomposeComponent.Factory {
    return DashboardDecomposeComponent.Factory(
        settingsViewModelFactory = { SettingsDashboardViewModel(fFeatureProvider) },
        deviceInfoViewModelFactory = { DeviceInfoDashboardViewModel(fFeatureProvider) },
        accountViewModelFactory = { AccountDashboardViewModel(fFeatureProvider, principalApi) },
        hardwareViewModelFactory = { HardwareDashboardViewModel(fFeatureProvider) },
        onCallViewModelFactory = { OnCallDashboardViewModel(fFeatureProvider) },
        smartHomeViewModelFactory = { SmartHomeDashboardViewModel(fFeatureProvider) },
        timezoneViewModelFactory = { TimezoneDashboardViewModel(fFeatureProvider) },
        assetsViewModelFactory = { AssetsDashboardViewModel(fFeatureProvider) },
        displayViewModelFactory = { DisplayDashboardViewModel(fFeatureProvider) },
        screenStreamingViewModelFactory = { ScreenStreamingDashboardViewModel(fFeatureProvider) },
        firmwareUpdateViewModelFactory = { FirmwareUpdateViewModel(firmwareUpdaterApi) }
    )
}
