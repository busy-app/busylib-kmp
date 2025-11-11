package net.flipper.bridge.connection.feature.wifi.impl

import net.flipper.bridge.connection.feature.rpc.api.model.WifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import kotlin.test.Test
import kotlin.test.assertEquals

class WiFiNetworkReplaceComparatorTest {
    @Test
    fun supportedWifiFirst() {
        val list = listOf(
            WiFiNetwork(
                ssid = "Test",
                rssi = 12,
                wifiSecurity = WiFiSecurity.Other(
                    WifiSecurityMethod.WPA3_ENTERPRISE
                )
            ),
            WiFiNetwork(
                ssid = "Test",
                rssi = 10,
                wifiSecurity = WiFiSecurity.Supported.None
            ),
        )
        assertEquals(
            WiFiNetwork(
                ssid = "Test",
                rssi = 10,
                wifiSecurity = WiFiSecurity.Supported.None
            ),
            list.maxWith(WiFiNetworkReplaceComparator())
        )
    }

    @Test
    fun supportedWifiFirstWithSameRssi() {
        val list = listOf(
            WiFiNetwork(
                ssid = "Test",
                rssi = 10,
                wifiSecurity = WiFiSecurity.Other(
                    WifiSecurityMethod.WPA3_ENTERPRISE
                )
            ),
            WiFiNetwork(
                ssid = "Test",
                rssi = 10,
                wifiSecurity = WiFiSecurity.Supported.None
            ),
        )
        assertEquals(
            WiFiNetwork(
                ssid = "Test",
                rssi = 10,
                wifiSecurity = WiFiSecurity.Supported.None
            ),
            list.maxWith(WiFiNetworkReplaceComparator())
        )
    }

    @Test
    fun higherRssiFirst() {
        val list = listOf(
            WiFiNetwork(
                ssid = "Test",
                rssi = 8,
                wifiSecurity = WiFiSecurity.Supported.None
            ),
            WiFiNetwork(
                ssid = "Test",
                rssi = 10,
                wifiSecurity = WiFiSecurity.Supported.None
            ),
        )
        assertEquals(
            WiFiNetwork(
                ssid = "Test",
                rssi = 10,
                wifiSecurity = WiFiSecurity.Supported.None
            ),
            list.maxWith(WiFiNetworkReplaceComparator())
        )
    }
}
