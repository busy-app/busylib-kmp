package net.flipper.bridge.connection.feature.wifi.impl

import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity

@Suppress("FunctionNaming")
internal fun WiFiNetworkSortComparator(): Comparator<WiFiNetwork> {
    return compareByDescending<WiFiNetwork> { it.wifiSecurity is WiFiSecurity.Supported }
        .thenByDescending { it.rssi }
}
