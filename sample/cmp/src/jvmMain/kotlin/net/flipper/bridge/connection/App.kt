package net.flipper.bridge.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.flipperdevices.core.network.BUSYLibNetworkStateApiNoop
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.flipper.bridge.connection.config.impl.FDevicePersistedStorageImpl
import net.flipper.bridge.connection.screens.di.getRootDecomposeComponent
import net.flipper.bridge.connection.screens.search.LanSearchViewModel
import net.flipper.bridge.connection.utils.PermissionCheckerNoop
import net.flipper.bridge.connection.utils.Secrets
import net.flipper.bridge.connection.utils.cloud.BUSYLibBarsApiNoop
import net.flipper.bridge.connection.utils.getUserPrincipal
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import net.flipper.bridge.connection.utils.runOnUiThread
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApiStub
import net.flipper.busylib.BUSYLibDesktop
import kotlinx.coroutines.launch
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import java.util.prefs.Preferences

suspend fun main() {
    val lifecycle = LifecycleRegistry()
    val applicationScope = CoroutineScope(
        SupervisorJob() + FlipperDispatchers.default
    )

    val persistedStorage = FDevicePersistedStorageImpl(
        PreferencesSettings(Preferences.userRoot())
    )
    val hostApi = BUSYLibHostApiStub(
        host = "cloud.dev.busy.app",
        proxyHost = "proxy.dev.busy.app"
    )
    val busyLib = BUSYLibDesktop.build(
        scope = applicationScope,
        principalApi = UserPrincipalApiNoop(
            getUserPrincipal(hostApi)
        ),
        busyLibBarsApi = BUSYLibBarsApiNoop(),
        persistedStorage = persistedStorage,
        networkStateApi = BUSYLibNetworkStateApiNoop(defaultState = true),
        hostApi = hostApi
    )

    busyLib.launch()

    applicationScope.launch {
        val principal = getUserPrincipal(hostApi)
        println("UserPrincipal: $principal")
    }

    val root = runOnUiThread {
        getRootDecomposeComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            permissionChecker = PermissionCheckerNoop(),
            persistedStorage = persistedStorage,
            busyLib = busyLib,
            searchViewModelProvider = {
                LanSearchViewModel(persistedStorage)
            }
        )
    }

    application {
        val windowState = rememberWindowState()

        LifecycleController(lifecycle, windowState)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "BUSY Bar App",
        ) {
            MaterialTheme {
                MaterialTheme(lightColors()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colors.background)
                            .safeDrawingPadding()
                    ) {
                        root.Render(Modifier)
                    }
                }
            }
        }
    }
}
