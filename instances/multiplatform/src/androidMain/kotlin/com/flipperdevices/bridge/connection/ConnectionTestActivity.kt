package com.flipperdevices.bridge.connection

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.defaultComponentContext
import com.flipperdevices.bridge.connection.screens.di.getRootDecomposeComponent
import com.flipperdevices.bridge.connection.screens.search.SampleBLESearchViewModel
import com.flipperdevices.bridge.connection.utils.PermissionCheckerImpl

class ConnectionTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootModule = (application as ConnectionTestApplication).rootModule
        val persistedStorage = (application as ConnectionTestApplication).persistedStorage

        enableEdgeToEdge()

        val root = getRootDecomposeComponent(
            componentContext = defaultComponentContext(),
            rootModule = rootModule,
            permissionChecker = PermissionCheckerImpl(this),
            persistedStorage = persistedStorage,
            searchViewModelProvider = {
                SampleBLESearchViewModel(
                    persistedStorage = persistedStorage,
                    flipperScanner = rootModule.scannerModule.flipperScanner
                )
            }
        )

        setContent {
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
