package net.flipper.bridge.connection.screens.device.viewmodel

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.bridge.connection.service.api.FConnectionService

class FCurrentDeviceViewModel(
    private val orchestrator: FDeviceOrchestrator,
    private val service: FConnectionService,
    private val fDevicePersistedStorage: FDevicePersistedStorage,
) : DecomposeViewModel() {
    fun getState() = orchestrator.getState()

    fun connect() = viewModelScope.launch {
        service.connectCurrent()
    }

    fun forget() = viewModelScope.launch {
        val device = fDevicePersistedStorage.getCurrentDeviceFlow()
            .firstOrNull()
            ?: return@launch
        service.forgetDevice(device)
    }

    fun disconnect() = viewModelScope.launch {
        service.disconnect()
    }
}
