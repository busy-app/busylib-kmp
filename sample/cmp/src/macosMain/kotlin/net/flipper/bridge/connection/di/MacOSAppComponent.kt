package net.flipper.bridge.connection.di

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.flipper.bridge.connection.config.impl.FDevicePersistedStorageImpl
import net.flipper.bridge.connection.screens.ConnectionRootDecomposeComponent
import net.flipper.bridge.connection.screens.di.getRootDecomposeComponent
import net.flipper.bridge.connection.screens.search.LanSearchViewModel
import net.flipper.bridge.connection.utils.PermissionCheckerNoop
import net.flipper.bridge.connection.utils.cloud.BUSYLibBarsApiNoop
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import net.flipper.busylib.BUSYLibMacOS
import platform.Foundation.NSUserDefaults

val storage by lazy {
    FDevicePersistedStorageImpl(
        NSUserDefaultsSettings(
            NSUserDefaults.standardUserDefaults
        )
    )
}
val busyLib: BUSYLibMacOS by lazy {
    BUSYLibMacOS.build(
        CoroutineScope(SupervisorJob()),
        principalApi = UserPrincipalApiNoop(),
        busyLibBarsApi = BUSYLibBarsApiNoop(),
        persistedStorage = storage,
    )
}

fun getRootDecomposeComponent(): ConnectionRootDecomposeComponent {
    val lifecycle = LifecycleRegistry()
    val componentContext: ComponentContext = DefaultComponentContext(
        lifecycle = lifecycle
    )
    return getRootDecomposeComponent(
        componentContext = componentContext,
        busyLib = busyLib,
        permissionChecker = PermissionCheckerNoop(),
        persistedStorage = storage,
        searchViewModelProvider = {
            LanSearchViewModel(
                storage
            )
        }
    )
}
