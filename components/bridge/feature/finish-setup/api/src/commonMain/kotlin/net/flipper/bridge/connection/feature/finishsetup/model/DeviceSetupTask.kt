package net.flipper.bridge.connection.feature.finishsetup.model

data class DeviceSetupTask(
    val type: DeviceSetupTaskType,
    val status: DeviceSetupTaskStatus,
)
