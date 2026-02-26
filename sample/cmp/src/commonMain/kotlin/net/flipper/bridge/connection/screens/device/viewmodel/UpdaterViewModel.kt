package net.flipper.bridge.connection.screens.device.viewmodel

import kotlinx.coroutines.launch
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi

class UpdaterViewModel(
    private val firmwareUpdaterApi: FirmwareUpdaterApi,
) : DecomposeViewModel() {
    fun startUpdate() {
        viewModelScope.launch {
            firmwareUpdaterApi.startUpdateInstall()
        }
    }
}
