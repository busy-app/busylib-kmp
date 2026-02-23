package net.flipper.bridge.connection.feature.finishsetup.model

sealed interface FFinishSetupState {
    data object Loading : FFinishSetupState
    data object FinishedBefore : FFinishSetupState
    data class Loaded(val tasks: List<DeviceSetupTask>) : FFinishSetupState
}
