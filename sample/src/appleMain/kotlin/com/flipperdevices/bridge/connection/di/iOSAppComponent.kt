package com.flipperdevices.bridge.connection.di

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.flipperdevices.bridge.connection.screens.ConnectionRootDecomposeComponent
import com.flipperdevices.bridge.connection.screens.di.getRootDecomposeComponent
import com.flipperdevices.bridge.connection.screens.search.iOSSearchViewModel
import com.flipperdevices.bridge.connection.utils.PermissionCheckerNoop
import com.flipperdevices.bridge.connection.utils.cloud.BSBBarsApiNoop
import com.flipperdevices.bridge.connection.utils.config.impl.FDevicePersistedStorageImpl
import com.flipperdevices.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import com.flipperdevices.busylib.BUSYLibIOS
import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import platform.Foundation.NSUserDefaults

val storage by lazy {
    FDevicePersistedStorageImpl(
        NSUserDefaultsSettings(
            NSUserDefaults.standardUserDefaults
        )
    )
}
val busyLib: BUSYLibIOS by lazy {
    BUSYLibIOS.build(
        CoroutineScope(SupervisorJob()),
        principalApi = UserPrincipalApiNoop(),
        bsbBarsApi = BSBBarsApiNoop(),
        persistedStorage = storage
    )
}

fun getRootDecomposeComponent(): ConnectionRootDecomposeComponent {
    val componentContext: ComponentContext = DefaultComponentContext(
        lifecycle = ApplicationLifecycle()
    )
    return getRootDecomposeComponent(
        componentContext = componentContext,
        busyLib = busyLib,
        permissionChecker = PermissionCheckerNoop(),
        persistedStorage = storage,
        searchViewModelProvider = {
            iOSSearchViewModel(
                storage
            )
        }
    )
}