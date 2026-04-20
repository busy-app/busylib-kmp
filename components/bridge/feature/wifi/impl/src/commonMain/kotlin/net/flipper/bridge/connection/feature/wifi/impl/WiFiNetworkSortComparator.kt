package net.flipper.bridge.connection.feature.wifi.impl

import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork

@Suppress("FunctionNaming")
internal fun WiFiNetworkSortComparator(): Comparator<WiFiNetwork> {
    return Comparator { first, second -> second.rssi.compareTo(first.rssi) }
}
