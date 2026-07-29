package net.flipper.bridge.connection.screens.di

import com.arkivanov.decompose.ComponentContext
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.dashboard.account.AccountDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.assets.AssetsDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.deviceinfo.DeviceInfoDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.display.DisplayDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.drawtool.DrawToolCollectionSourceResolver
import net.flipper.bridge.connection.screens.dashboard.drawtool.DrawToolDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.drawtool.DrawToolSampleStatusWriter
import net.flipper.bridge.connection.screens.dashboard.hardware.HardwareDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.oncall.OnCallDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.root.DashboardDecomposeComponent
import net.flipper.bridge.connection.screens.dashboard.screenstreaming.ScreenStreamingDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.settings.SettingsDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.smarthome.SmartHomeDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.timezone.TimezoneDashboardViewModel
import net.flipper.bridge.connection.screens.dashboard.wifi.WiFiDashboardViewModel
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
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.busylib.BUSYLib
import net.flipper.core.busylib.ktx.io.SystemFlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.collection.util.DrawToolFileTypeResolver
import net.flipper.tools.drawtool.collection.util.DrawToolStatusIdValidator
import net.flipper.tools.drawtool.status.util.DrawToolStatusIdGenerator
import net.flipper.tools.multistream.api.MultiStreamApi

fun getRootDecomposeComponent(
    componentContext: ComponentContext,
    permissionChecker: PermissionChecker,
    persistedStorage: FDevicePersistedStorage,
    busyLib: BUSYLib,
    searchViewModelProvider: () -> ConnectionSearchViewModel,
    principalApi: UserPrincipalApiSampleImpl,
    busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
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
        multiStreamApi = busyLib.multiStreamApi,
        busyFirmwareDirectoryChannelApi = busyFirmwareDirectoryChannelApi,
        drawToolStatusesApi = busyLib.drawToolStatusesApi
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
    multiStreamApi: MultiStreamApi,
    busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    drawToolStatusesApi: DrawToolStatusesApi,
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
            firmwareUpdaterApi = firmwareUpdaterApi,
            busyFirmwareDirectoryChannelApi = busyFirmwareDirectoryChannelApi,
            persistedStorage = persistedStorage,
            drawToolStatusesApi = drawToolStatusesApi
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

private fun getDrawToolViewModelFactory(
    fFeatureProvider: FFeatureProvider,
    persistedStorage: FDevicePersistedStorage,
    drawToolStatusesApi: DrawToolStatusesApi,
): () -> DrawToolDashboardViewModel {
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    val collectionSourceResolver = DrawToolCollectionSourceResolver(
        featureProvider = fFeatureProvider,
        clientStatusesApi = drawToolStatusesApi,
        clientFileSystem = SystemFlipperFileSystem(delegate = SystemFileSystem),
        json = json,
        statusIdValidator = DrawToolStatusIdValidator(),
        fileTypeResolver = DrawToolFileTypeResolver()
    )
    val statusWriter = DrawToolSampleStatusWriter(
        json = json,
        statusIdGenerator = DrawToolStatusIdGenerator()
    )
    return {
        DrawToolDashboardViewModel(
            featureProvider = fFeatureProvider,
            persistedStorage = persistedStorage,
            collectionSourceResolver = collectionSourceResolver,
            statusWriter = statusWriter
        )
    }
}

private fun getDashboardDecomposeComponentFactory(
    fFeatureProvider: FFeatureProvider,
    principalApi: UserPrincipalApiSampleImpl,
    firmwareUpdaterApi: FirmwareUpdaterApi,
    busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    persistedStorage: FDevicePersistedStorage,
    drawToolStatusesApi: DrawToolStatusesApi,
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
        drawToolViewModelFactory = getDrawToolViewModelFactory(
            fFeatureProvider = fFeatureProvider,
            persistedStorage = persistedStorage,
            drawToolStatusesApi = drawToolStatusesApi
        ),
        screenStreamingViewModelFactory = { ScreenStreamingDashboardViewModel(fFeatureProvider) },
        wifiViewModelFactory = { WiFiDashboardViewModel(fFeatureProvider) },
        firmwareUpdateViewModelFactory = { FirmwareUpdateViewModel(firmwareUpdaterApi) },
        busyFirmwareDirectoryChannelApi = busyFirmwareDirectoryChannelApi
    )
}
