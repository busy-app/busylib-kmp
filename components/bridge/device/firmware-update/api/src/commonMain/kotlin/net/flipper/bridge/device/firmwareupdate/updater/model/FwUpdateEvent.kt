package net.flipper.bridge.device.firmwareupdate.updater.model

sealed interface FwUpdateEvent {
    data object UpdateFinished : FwUpdateEvent

    data object UpdateFailed : FwUpdateEvent
}
