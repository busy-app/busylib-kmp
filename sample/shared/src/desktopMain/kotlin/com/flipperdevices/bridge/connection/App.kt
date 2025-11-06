package com.flipperdevices.bridge.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.flipperdevices.bridge.connection.di.AppComponent
import com.flipperdevices.bridge.connection.utils.runOnUiThread
import com.flipperdevices.bsb.core.theme.BusyBarTheme
import com.flipperdevices.bsb.core.theme.LocalV2Pallet

fun launch(appComponent: AppComponent) {
    val lifecycle = LifecycleRegistry()
    val root = runOnUiThread {
        appComponent.rootComponentFactory(
            DefaultComponentContext(lifecycle = lifecycle)
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
            BusyBarTheme(darkMode = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalV2Pallet.current.background.base)
                        .safeDrawingPadding()
                ) {
                    root.Render(Modifier)
                }
            }
        }
    }
}
