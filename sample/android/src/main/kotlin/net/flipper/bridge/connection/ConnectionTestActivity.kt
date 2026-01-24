package net.flipper.bridge.connection

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
import net.flipper.bridge.connection.screens.di.getRootDecomposeComponent
import net.flipper.bridge.connection.screens.search.SampleBLESearchViewModel
import net.flipper.bridge.connection.utils.PermissionCheckerImpl

class ConnectionTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val busyLib = (application as ConnectionTestApplication).busyLib
        val persistedStorage = (application as ConnectionTestApplication).persistedStorage

        enableEdgeToEdge()

        val root = getRootDecomposeComponent(
            componentContext = defaultComponentContext(),
            busyLib = busyLib,
            permissionChecker = PermissionCheckerImpl(this),
            persistedStorage = persistedStorage,
            searchViewModelProvider = {
                SampleBLESearchViewModel(
                    persistedStorage,
                    busyLib.flipperScanner
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
