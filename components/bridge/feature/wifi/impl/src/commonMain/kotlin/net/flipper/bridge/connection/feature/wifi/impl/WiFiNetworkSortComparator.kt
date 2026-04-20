package net.flipper.bridge.connection.feature.wifi.impl

import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork

@Suppress("FunctionNaming")
internal fun WiFiNetworkSortComparator(): Comparator<WiFiNetwork> {
    return Comparator { a, b -> compareNullableRssiDescending(a.rssi, b.rssi) }
}

private fun compareNullableRssiDescending(first: Int?, second: Int?): Int {
    return when {
        first == second -> 0
        first == null -> 1
        second == null -> -1
        first > second -> -1
        else -> 1
    }
}
