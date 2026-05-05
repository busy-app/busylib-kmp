package net.flipper.bridge.connection.feature.battery.model

import net.flipper.core.busylib.data.Fraction

data class BSBDeviceBatteryInfo(
    val state: BSBBatteryState,
    val percentage: Fraction
) {
    enum class BSBBatteryState {
        DISCHARGING,
        CHARGING,
        CHARGED
    }
}
