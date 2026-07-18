package net.flipper.bridge.connection.screens.fwupdate

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import busylibkmp.sample.cmp.generated.resources.Res
import busylibkmp.sample.cmp.generated.resources.ic_more
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId
import org.jetbrains.compose.resources.painterResource

class FirmwareUpdateDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    private val firmwareUpdateViewModelFactory: () -> FirmwareUpdateViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        firmwareUpdateViewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val state by viewModel.stateFlow.collectAsState()
        val lastEvent by viewModel.lastEventFlow.collectAsState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Firmware Update",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onBackground
            )
            FirmwareChannelDropdown()
            Spacer(Modifier.height(8.dp))

            StateSection(state)

            Spacer(Modifier.height(8.dp))

            lastEvent?.let { event ->
                EventBanner(event)
                Spacer(Modifier.height(8.dp))
            }

            ActionButtons(state)

            Spacer(Modifier.weight(1f))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onBack() }
            ) {
                Text("Back")
            }
        }
    }

    @Suppress("LongMethod")
    @Composable
    private fun StateSection(state: FwUpdateState) {
        Text(
            text = "Status: ${stateLabel(state)}",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground
        )

        when (state) {
            is FwUpdateState.Downloading -> {
                Text(
                    text = "Downloading: ${(state.progress * 100).toInt()}%",
                    color = MaterialTheme.colors.onBackground
                )
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is FwUpdateState.Uploading -> {
                Text(
                    text = "Uploading: ${(state.progress * 100).toInt()}%",
                    color = MaterialTheme.colors.onBackground
                )
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FwUpdateState.CheckingVersion -> {
                Text(
                    text = "Checking for updates...",
                    color = MaterialTheme.colors.onBackground
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            FwUpdateState.Updating -> {
                Text(
                    text = "Installing update on device...",
                    color = MaterialTheme.colors.onBackground
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            FwUpdateState.CouldNotCheckUpdate -> {
                Text(
                    text = "Could not check for updates. Check connection.",
                    color = Color.Red
                )
            }

            FwUpdateState.Pending -> {
                Text(
                    text = "Waiting...",
                    color = MaterialTheme.colors.onBackground
                )
            }

            FwUpdateState.NoUpdateAvailable -> {
                Text(
                    text = "Device is up to date.",
                    color = MaterialTheme.colors.onBackground
                )
            }

            FwUpdateState.UpdateAvailable -> {
                Text(
                    text = "A new firmware version is available.",
                    color = MaterialTheme.colors.onBackground
                )
            }

            FwUpdateState.DownloadFailure -> {
                Text(
                    text = "Download failure happened!",
                    color = MaterialTheme.colors.error
                )
            }

            FwUpdateState.Preparing -> {
                Text(
                    text = "Preparing...",
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }

    @Composable
    private fun EventBanner(event: FwUpdateEvent) {
        val (text, color) = when (event) {
            FwUpdateEvent.UpdateFinished -> "Update completed successfully!" to Color.Green
            FwUpdateEvent.UpdateFailed -> "Update failed!" to Color.Red
        }
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.subtitle1
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun FirmwareChannelDropdown() {
        val scope = rememberCoroutineScope()
        val currentChannel by busyFirmwareDirectoryChannelApi.getChannelIdFlow().collectAsState()
        var isExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = true }
                .border(1.dp, Color.Gray),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = !isExpanded },
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
            ) {
                Text(
                    text = currentChannel.id,
                    color = MaterialTheme.colors.onBackground
                )
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    BsbFirmwareChannelId.entries.forEach { channel ->
                        DropdownMenuItem(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                isExpanded = false
                                scope.launch {
                                    busyFirmwareDirectoryChannelApi.setChannel(channel)
                                }
                            }
                        ) {
                            Text(
                                text = channel.id,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painterResource(Res.drawable.ic_more),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground
            )
        }
    }

    @Composable
    private fun ActionButtons(state: FwUpdateState) {
        val canStart = state is FwUpdateState.UpdateAvailable ||
            state is FwUpdateState.CouldNotCheckUpdate

        val canStop = state is FwUpdateState.Downloading ||
            state is FwUpdateState.Uploading ||
            state is FwUpdateState.Updating

        if (canStart) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::startUpdate
            ) {
                Text("Start Update")
            }
        }

        if (canStop) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::stopUpdate,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red
                )
            ) {
                Text("Stop Update", color = Color.White)
            }
        }
    }

    private fun stateLabel(state: FwUpdateState): String = when (state) {
        FwUpdateState.CheckingVersion -> "Checking Version"
        FwUpdateState.Pending -> "Pending"
        FwUpdateState.CouldNotCheckUpdate -> "Could Not Check"
        FwUpdateState.NoUpdateAvailable -> "No Update"
        FwUpdateState.Updating -> "Updating"
        FwUpdateState.UpdateAvailable -> "Update Available"
        is FwUpdateState.Uploading -> "Uploading"
        is FwUpdateState.Downloading -> "Downloading"
        FwUpdateState.DownloadFailure -> "Download failure"
        FwUpdateState.Preparing -> "Preparing"
    }

    class Factory(
        private val firmwareUpdateViewModelFactory: () -> FirmwareUpdateViewModel,
        private val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi
    ) {
        fun invoke(
            componentContext: ComponentContext,
            onBack: DecomposeOnBackParameter,
        ): FirmwareUpdateDecomposeComponent {
            return FirmwareUpdateDecomposeComponent(
                componentContext = componentContext,
                onBack = onBack,
                firmwareUpdateViewModelFactory = firmwareUpdateViewModelFactory,
                busyFirmwareDirectoryChannelApi = busyFirmwareDirectoryChannelApi
            )
        }
    }
}
