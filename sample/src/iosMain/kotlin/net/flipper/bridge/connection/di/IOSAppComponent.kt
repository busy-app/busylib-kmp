package net.flipper.bridge.connection.di

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.flipper.bridge.connection.config.impl.FDevicePersistedStorageImpl
import net.flipper.bridge.connection.screens.ConnectionRootDecomposeComponent
import net.flipper.bridge.connection.screens.di.getRootDecomposeComponent
import net.flipper.bridge.connection.screens.search.IOSSearchViewModel
import net.flipper.bridge.connection.utils.PermissionCheckerNoop
import net.flipper.bridge.connection.utils.cloud.BUSYLibBarsApiNoop
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import net.flipper.busylib.BUSYLibIOS
import platform.CoreBluetooth.CBCentralManager
import platform.Foundation.NSUserDefaults

val manager: CBCentralManager by lazy {
    CBCentralManager()
}

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
        busyLibBarsApi = BUSYLibBarsApiNoop(),
        persistedStorage = storage,
        manager = manager,
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
            IOSSearchViewModel(
                persistedStorage = storage,
                connectionService = busyLib.connectionService
            )
        }
    )
}
