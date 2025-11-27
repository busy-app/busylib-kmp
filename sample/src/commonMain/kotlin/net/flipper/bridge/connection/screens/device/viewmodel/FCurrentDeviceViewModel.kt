package net.flipper.bridge.connection.screens.device.viewmodel

import kotlinx.coroutines.launch
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel
import net.flipper.bridge.connection.service.api.FConnectionService

class FCurrentDeviceViewModel(
    private val orchestrator: FDeviceOrchestrator,
    private val service: FConnectionService
) : DecomposeViewModel() {
    fun getState() = orchestrator.getState()

    fun connect() = viewModelScope.launch {
        service.connectCurrent()
    }

    fun forget() = viewModelScope.launch {
        service.forgetCurrentDevice()
    }

    fun disconnect() = viewModelScope.launch {
        service.disconnect()
    }
}
