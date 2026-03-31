package net.flipper.bridge.connection.screens.fwupdate

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState

class FirmwareUpdateViewModel(
    private val firmwareUpdaterApi: FirmwareUpdaterApi
) : DecomposeViewModel() {
    val stateFlow: StateFlow<FwUpdateState> = firmwareUpdaterApi.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, FwUpdateState.Pending)

    val lastEventFlow: StateFlow<FwUpdateEvent?> = firmwareUpdaterApi.events
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun startUpdate() {
        viewModelScope.launch {
            firmwareUpdaterApi.startUpdateInstall()
        }
    }

    fun stopUpdate() {
        viewModelScope.launch {
            firmwareUpdaterApi.stopFirmwareUpdate()
        }
    }
}
