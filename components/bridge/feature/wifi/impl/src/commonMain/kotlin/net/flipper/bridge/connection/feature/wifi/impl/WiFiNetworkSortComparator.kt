package net.flipper.bridge.connection.feature.wifi.impl

import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity

@Suppress("FunctionNaming")
internal fun WiFiNetworkSortComparator(): Comparator<WiFiNetwork> {
    return compareBy<WiFiNetwork> { it.wifiSecurity.sortPriority() }
        .thenByDescending { it.rssi }
        .thenBy { it.ssid }
}

private fun WiFiSecurity.sortPriority(): Int = when (this) {
    is WiFiSecurity.Supported -> 0
    is WiFiSecurity.Other -> 1
}
