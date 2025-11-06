package com.flipperdevices.bridge.connection.feature.wifi.impl

import com.flipperdevices.bridge.connection.feature.wifi.api.model.WiFiNetwork
import com.flipperdevices.bridge.connection.feature.wifi.api.model.WiFiSecurity

@Suppress("FunctionNaming")
fun WiFiNetworkReplaceComparator(): Comparator<WiFiNetwork> {
    return Comparator<WiFiNetwork> { a, b ->
        when (a.wifiSecurity) {
            is WiFiSecurity.Other -> when (b.wifiSecurity) {
                is WiFiSecurity.Other -> 0
                is WiFiSecurity.Supported -> -1
            }

            is WiFiSecurity.Supported -> when (b.wifiSecurity) {
                is WiFiSecurity.Other -> 1
                is WiFiSecurity.Supported -> 0
            }
        }
    }.thenBy { it.rssi }
}
