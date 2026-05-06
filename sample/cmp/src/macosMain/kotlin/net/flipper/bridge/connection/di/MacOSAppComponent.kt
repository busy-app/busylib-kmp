package net.flipper.bridge.connection.di

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.flipper.bridge.connection.screens.di.getRootDecomposeComponent
import net.flipper.bridge.connection.screens.root.ConnectionRootDecomposeComponent
import net.flipper.bridge.connection.screens.search.LanSearchViewModel
import net.flipper.bridge.connection.utils.BUSYLibNetworkStateApple
import net.flipper.bridge.connection.utils.PermissionCheckerNoop
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiSampleImpl
import net.flipper.bsb.cloud.api.BUSYLibHostApiStub
import net.flipper.busylib.BUSYLibMacOS
import platform.Foundation.NSUserDefaults

private val settings by lazy {
    NSUserDefaultsSettings(
        NSUserDefaults.standardUserDefaults
    )
}
private val applicationScope by lazy {
    CoroutineScope(SupervisorJob())
}

private val hostApi = BUSYLibHostApiStub(
    host = "cloud.dev.busy.app",
)

private val principalApi by lazy {
    UserPrincipalApiSampleImpl(applicationScope, hostApi, settings)
}

val busyLib: BUSYLibMacOS by lazy {
    BUSYLibMacOS.build(
        applicationScope,
        principalApi = principalApi,
        observableSettings = settings,
        hostApi = hostApi,
        networkStateApi = BUSYLibNetworkStateApple(),
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
        persistedStorage = busyLib.persistedStorage,
        searchViewModelProvider = {
            LanSearchViewModel(
                busyLib.persistedStorage,
                busyLib.connectionService,
            )
        },
        principalApi = principalApi,
        busyFirmwareDirectoryChannelApi = busyLib.busyFirmwareDirectoryChannelApi
    )
}
