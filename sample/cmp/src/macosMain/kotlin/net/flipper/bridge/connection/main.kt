@file:Suppress("Filename")

package net.flipper.bridge.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import net.flipper.bridge.connection.screens.ConnectionRootDecomposeComponent

fun rootWindow(
    root: ConnectionRootDecomposeComponent
) {
    Window {
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
