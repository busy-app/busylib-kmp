package net.flipper.bridge.connection.screens.di

import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.ConnectionRootDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.DashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.account.AccountDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.deviceinfo.DeviceInfoDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.hardware.HardwareDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.oncall.OnCallDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.overview.OverviewDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.ScreenStreamingDashboardViewModel
import net.flipper.bridge.connection.screens.device.ConnectionDeviceScreenDecomposeComponent
import net.flipper.bridge.connection.screens.device.viewmodel.FCurrentDeviceViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.FDevicesViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.PingViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.UpdaterViewModel
import net.flipper.bridge.connection.screens.search.ConnectionSearchDecomposeComponent
import net.flipper.bridge.connection.screens.search.ConnectionSearchViewModel
import net.flipper.bridge.connection.screens.utils.PermissionChecker
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiSampleImpl
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi
import net.flipper.busylib.BUSYLib

fun getRootDecomposeComponent(
    componentContext: ComponentContext,
    permissionChecker: PermissionChecker,
    persistedStorage: FDevicePersistedStorage,
    busyLib: BUSYLib,
    searchViewModelProvider: () -> ConnectionSearchViewModel,
    principalApi: UserPrincipalApiSampleImpl? = null
): ConnectionRootDecomposeComponent {
    return getRootDecomposeComponentFactory(
        permissionChecker = permissionChecker,
        persistedStorage = persistedStorage,
        orchestrator = busyLib.orchestrator,
        featureProvider = busyLib.featureProvider,
        searchViewModelProvider = searchViewModelProvider,
        fConnectionService = busyLib.connectionService,
        firmwareUpdaterApi = busyLib.firmwareUpdaterApi,
        principalApi = principalApi
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
    principalApi: UserPrincipalApiSampleImpl?
): ConnectionRootDecomposeComponent.Factory {
    return ConnectionRootDecomposeComponent.Factory(
        permissionChecker = permissionChecker,
        searchDecomposeFactory = getSearchDecomposeFactory(
            searchViewModelProvider = searchViewModelProvider
        ),
        connectionDeviceScreenDecomposeComponentFactory = getConnectionDeviceScreenDecomposeComponentFactory(
            persistedStorage = persistedStorage,
            orchestrator = orchestrator,
            featureProvider = featureProvider,
            fService = fConnectionService,
            firmwareUpdaterApi = firmwareUpdaterApi
        ),
        dashboardDecomposeComponentFactory = getDashboardDecomposeComponentFactory(
            fFeatureProvider = featureProvider,
            principalApi = principalApi
        )
    )
}

private fun getSearchDecomposeFactory(
    searchViewModelProvider: () -> ConnectionSearchViewModel
): ConnectionSearchDecomposeComponent.Factory {
    return ConnectionSearchDecomposeComponent.Factory(
        searchViewModelProvider = searchViewModelProvider
    )
}

private fun getConnectionDeviceScreenDecomposeComponentFactory(
    persistedStorage: FDevicePersistedStorage,
    orchestrator: FDeviceOrchestrator,
    featureProvider: FFeatureProvider,
    fService: FConnectionService,
    firmwareUpdaterApi: FirmwareUpdaterApi
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
        updaterViewModelProvider = { UpdaterViewModel(firmwareUpdaterApi) }

    )
}

private fun getDashboardDecomposeComponentFactory(
    fFeatureProvider: FFeatureProvider,
    principalApi: UserPrincipalApiSampleImpl?
): DashboardDecomposeComponent.Factory {
    return DashboardDecomposeComponent.Factory(
        overviewViewModelFactory = { OverviewDashboardViewModel(fFeatureProvider) },
        deviceInfoViewModelFactory = { DeviceInfoDashboardViewModel(fFeatureProvider) },
        accountViewModelFactory = { AccountDashboardViewModel(fFeatureProvider, principalApi) },
        hardwareViewModelFactory = { HardwareDashboardViewModel(fFeatureProvider) },
        onCallViewModelFactory = { OnCallDashboardViewModel(fFeatureProvider) },
        screenStreamingViewModelFactory = { ScreenStreamingDashboardViewModel(fFeatureProvider) }
    )
}
