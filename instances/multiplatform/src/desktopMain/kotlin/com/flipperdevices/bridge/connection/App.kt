package com.flipperdevices.bridge.connection

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
import com.flipperdevices.bridge.connection.screens.di.getRootDecomposeComponent
import com.flipperdevices.bridge.connection.screens.search.USBSearchViewModel
import com.flipperdevices.bridge.connection.utils.PermissionCheckerNoop
import com.flipperdevices.bridge.connection.utils.cloud.BSBBarsApiNoop
import com.flipperdevices.bridge.connection.utils.config.impl.FDevicePersistedStorageImpl
import com.flipperdevices.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import com.flipperdevices.bridge.connection.utils.runOnUiThread
import com.flipperdevices.busylib.BUSYLibDesktop
import com.flipperdevices.core.busylib.ktx.common.FlipperDispatchers
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.prefs.Preferences

fun main() {
    val lifecycle = LifecycleRegistry()
    val applicationScope = CoroutineScope(
        SupervisorJob() + FlipperDispatchers.default
    )

    val persistedStorage = FDevicePersistedStorageImpl(
        PreferencesSettings(Preferences.userRoot())
    )
    val busyLib = BUSYLibDesktop.build(
        scope = applicationScope,
        principalApi = UserPrincipalApiNoop(),
        bsbBarsApi = BSBBarsApiNoop(),
        persistedStorage = persistedStorage
    )

    val root = runOnUiThread {
        getRootDecomposeComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            permissionChecker = PermissionCheckerNoop(),
            persistedStorage = persistedStorage,
            busyLib = busyLib,
            searchViewModelProvider = {
                USBSearchViewModel(persistedStorage)
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
