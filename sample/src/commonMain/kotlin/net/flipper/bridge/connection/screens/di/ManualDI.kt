package net.flipper.bridge.connection.screens.di

import com.arkivanov.decompose.ComponentContext
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.ConnectionRootDecomposeComponent
import net.flipper.bridge.connection.screens.device.ConnectionDeviceScreenDecomposeComponent
import net.flipper.bridge.connection.screens.device.viewmodel.FCurrentDeviceViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.FDevicesViewModel
import net.flipper.bridge.connection.screens.device.viewmodel.PingViewModel
import net.flipper.bridge.connection.screens.search.ConnectionSearchDecomposeComponent
import net.flipper.bridge.connection.screens.search.ConnectionSearchViewModel
import net.flipper.bridge.connection.screens.utils.PermissionChecker
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.busylib.BUSYLib

fun getRootDecomposeComponent(
    componentContext: ComponentContext,
    permissionChecker: PermissionChecker,
    persistedStorage: FDevicePersistedStorage,
    busyLib: BUSYLib,
    searchViewModelProvider: () -> ConnectionSearchViewModel
): ConnectionRootDecomposeComponent {
    return getRootDecomposeComponentFactory(
        permissionChecker = permissionChecker,
        persistedStorage = persistedStorage,
        orchestrator = busyLib.orchestrator,
        featureProvider = busyLib.featureProvider,
        searchViewModelProvider = searchViewModelProvider,
        fConnectionService = busyLib.connectionService
    ).invoke(componentContext)
}

@Suppress("LongParameterList")
private fun getRootDecomposeComponentFactory(
    permissionChecker: PermissionChecker,
    persistedStorage: FDevicePersistedStorage,
    orchestrator: FDeviceOrchestrator,
    featureProvider: FFeatureProvider,
    fConnectionService: FConnectionService,
    searchViewModelProvider: () -> ConnectionSearchViewModel
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
            fService = fConnectionService
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
    fService: FConnectionService
): ConnectionDeviceScreenDecomposeComponent.Factory {
    return ConnectionDeviceScreenDecomposeComponent.Factory(
        devicesViewModelProvider = { FDevicesViewModel(persistedStorage) },
        currentDeviceViewModelProvider = {
            FCurrentDeviceViewModel(
                orchestrator,
                fService,
            )
        },
        pingViewModelProvider = { PingViewModel(featureProvider, orchestrator) }
    )
}
