package net.flipper.bridge.connection.screens.fwupdate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState

class FirmwareUpdateDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
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

            FwUpdateState.Failure -> {
                Text(
                    text = "Update failed. Try again.",
                    color = Color.Red
                )
            }

            FwUpdateState.LowBattery -> {
                Text(
                    text = "Battery too low to update. Please charge the device.",
                    color = Color.Red
                )
            }

            FwUpdateState.CouldNotCheckUpdate -> {
                Text(
                    text = "Could not check for updates. Check connection.",
                    color = Color.Red
                )
            }

            FwUpdateState.Busy -> {
                Text(
                    text = "Device is busy. Try again later.",
                    color = MaterialTheme.colors.onBackground
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
        }
    }

    @Composable
    private fun EventBanner(event: FwUpdateEvent) {
        val (text, color) = when (event) {
            FwUpdateEvent.UpdateFinished -> "Update completed successfully!" to Color(0xFF4CAF50)
            FwUpdateEvent.UpdateFailed -> "Update failed!" to Color.Red
        }
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.subtitle1
        )
    }

    @Composable
    private fun ActionButtons(state: FwUpdateState) {
        val canStart = state is FwUpdateState.UpdateAvailable ||
            state is FwUpdateState.Failure ||
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
        FwUpdateState.Failure -> "Failure"
        FwUpdateState.CheckingVersion -> "Checking Version"
        FwUpdateState.LowBattery -> "Low Battery"
        FwUpdateState.Busy -> "Busy"
        FwUpdateState.Pending -> "Pending"
        FwUpdateState.CouldNotCheckUpdate -> "Could Not Check"
        FwUpdateState.NoUpdateAvailable -> "No Update"
        FwUpdateState.Updating -> "Updating"
        FwUpdateState.UpdateAvailable -> "Update Available"
        is FwUpdateState.Uploading -> "Uploading"
        is FwUpdateState.Downloading -> "Downloading"
    }

    class Factory(
        private val firmwareUpdateViewModelFactory: () -> FirmwareUpdateViewModel
    ) {
        fun invoke(
            componentContext: ComponentContext,
            onBack: DecomposeOnBackParameter
        ): FirmwareUpdateDecomposeComponent {
            return FirmwareUpdateDecomposeComponent(
                componentContext = componentContext,
                onBack = onBack,
                firmwareUpdateViewModelFactory = firmwareUpdateViewModelFactory
            )
        }
    }
}
