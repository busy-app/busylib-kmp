package com.flipperdevices.bridge.connection.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.flipperdevices.bridge.connection.screens.decompose.DecomposeOnBackParameter
import com.flipperdevices.bridge.connection.screens.decompose.ScreenDecomposeComponent

class ConnectionSearchDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val searchViewModelProvider: () -> ConnectionSearchViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val searchViewModel = instanceKeeper.getOrCreate {
        searchViewModelProvider.invoke()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val devices by searchViewModel.getDevicesFlow().collectAsState()

        Column {
            Row {
                Icon(
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(16.dp)
                        .size(24.dp),
                    painter = rememberVectorPainter(Icons.Default.Close),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Search",
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onBackground
                )
            }
            LazyColumn {
                items(
                    devices,
                    key = { device -> device.address }
                ) { searchItem ->
                    Row {
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            text = searchItem.deviceModel.humanReadableName,
                            color = MaterialTheme.colors.onBackground
                        )

                        Icon(
                            modifier = Modifier
                                .clickable { searchViewModel.onDeviceClick(searchItem) }
                                .padding(16.dp)
                                .size(24.dp),
                            painter = rememberVectorPainter(
                                if (searchItem.isAdded) {
                                    Icons.Default.Delete
                                } else {
                                    Icons.Default.Add
                                }
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                }
            }
        }
    }

    class Factory(
        private val searchViewModelProvider: () -> ConnectionSearchViewModel
    ) {
        fun invoke(
            componentContext: ComponentContext,
            onBack: DecomposeOnBackParameter
        ): ConnectionSearchDecomposeComponent {
            return ConnectionSearchDecomposeComponent(
                componentContext,
                onBack,
                searchViewModelProvider
            )
        }
    }
}
