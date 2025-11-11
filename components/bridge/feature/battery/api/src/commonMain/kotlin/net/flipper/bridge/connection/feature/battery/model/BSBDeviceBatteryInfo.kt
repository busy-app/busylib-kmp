package net.flipper.bridge.connection.feature.battery.model

class BSBDeviceBatteryInfo(
    val state: BSBBatteryState,
    val percentage: Int
) {
    enum class BSBBatteryState {
        DISCHARGING,
        CHARGING,
        CHARGED
    }
}
