@file:Suppress("Filename")

package com.flipperdevices.bridge.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.flipperdevices.bridge.connection.screens.ConnectionRootDecomposeComponent
import platform.UIKit.UIViewController

fun rootViewController(root: ConnectionRootDecomposeComponent): UIViewController =
    ComposeUIViewController {
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
