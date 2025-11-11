package net.flipper.bridge.connection.screens.device.viewmodel

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.screens.decompose.DecomposeViewModel

class FCurrentDeviceViewModel(
    private val orchestrator: FDeviceOrchestrator,
    persistedStorage: FDevicePersistedStorage
) : DecomposeViewModel() {

    init {
        persistedStorage.getCurrentDevice()
            .onEach { device ->
                if (device == null) {
                    orchestrator.disconnectCurrent()
                } else {
                    orchestrator.connect(device)
                }
            }.launchIn(viewModelScope)
    }

    fun getState() = orchestrator.getState()
}
