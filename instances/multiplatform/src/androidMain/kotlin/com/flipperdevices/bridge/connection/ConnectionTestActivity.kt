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
import com.flipperdevices.bridge.connection.di.AppComponent
import com.flipperdevices.busylib.core.di.BusyLibComponentHolder

class ConnectionTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootComponentFactory = BusyLibComponentHolder.component<AppComponent>().rootComponentFactory

        enableEdgeToEdge()

        val root = rootComponentFactory(
            componentContext = defaultComponentContext(),
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
