package com.flipperdevices.bridge.connection.screens.di

import com.arkivanov.decompose.ComponentContext
import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
import com.flipperdevices.bridge.connection.feature.provider.api.FFeatureProvider
import com.flipperdevices.bridge.connection.orchestrator.api.FDeviceOrchestrator
import com.flipperdevices.bridge.connection.screens.ConnectionRootDecomposeComponent
import com.flipperdevices.bridge.connection.screens.device.ConnectionDeviceScreenDecomposeComponent
import com.flipperdevices.bridge.connection.screens.device.viewmodel.FCurrentDeviceViewModel
import com.flipperdevices.bridge.connection.screens.device.viewmodel.FDevicesViewModel
import com.flipperdevices.bridge.connection.screens.device.viewmodel.PingViewModel
import com.flipperdevices.bridge.connection.screens.search.ConnectionSearchDecomposeComponent
import com.flipperdevices.bridge.connection.screens.search.ConnectionSearchViewModel
import com.flipperdevices.bridge.connection.screens.utils.PermissionChecker
import com.flipperdevices.busylib.BUSYLib

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
        searchViewModelProvider = searchViewModelProvider
    ).invoke(componentContext)
}

private fun getRootDecomposeComponentFactory(
    permissionChecker: PermissionChecker,
    persistedStorage: FDevicePersistedStorage,
    orchestrator: FDeviceOrchestrator,
    featureProvider: FFeatureProvider,
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
            featureProvider = featureProvider
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
    featureProvider: FFeatureProvider
): ConnectionDeviceScreenDecomposeComponent.Factory {
    return ConnectionDeviceScreenDecomposeComponent.Factory(
        devicesViewModelProvider = { FDevicesViewModel(persistedStorage) },
        currentDeviceViewModelProvider = {
            FCurrentDeviceViewModel(
                orchestrator,
                persistedStorage
            )
        },
        pingViewModelProvider = { PingViewModel(featureProvider, orchestrator) }
    )
}