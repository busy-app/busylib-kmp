package net.flipper.bridge.connection.feature.wifi.impl

import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import kotlin.test.Test
import kotlin.test.assertEquals

class WiFiNetworkSortComparatorTest {

    @Test
    fun supportedGoesBeforeOther() {
        val networks = listOf(
            wifiNetwork(ssid = "other", rssi = -40, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3)),
            wifiNetwork(ssid = "supported", rssi = -80, security = WiFiSecurity.Supported.Password.WPA2)
        )

        assertEquals(
            listOf(
                wifiNetwork(ssid = "supported", rssi = -80, security = WiFiSecurity.Supported.Password.WPA2),
                wifiNetwork(ssid = "other", rssi = -40, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3))
            ),
            networks.sortedWith(WiFiNetworkSortComparator())
        )
    }

    @Test
    fun supportedSortedByRssiDescending() {
        val networks = listOf(
            wifiNetwork(ssid = "weak", rssi = -80, security = WiFiSecurity.Supported.Password.WPA2),
            wifiNetwork(ssid = "strong", rssi = -40, security = WiFiSecurity.Supported.Password.WPA3),
            wifiNetwork(ssid = "mid", rssi = -60, security = WiFiSecurity.Supported.None)
        )

        assertEquals(
            listOf(
                wifiNetwork(ssid = "strong", rssi = -40, security = WiFiSecurity.Supported.Password.WPA3),
                wifiNetwork(ssid = "mid", rssi = -60, security = WiFiSecurity.Supported.None),
                wifiNetwork(ssid = "weak", rssi = -80, security = WiFiSecurity.Supported.Password.WPA2)
            ),
            networks.sortedWith(WiFiNetworkSortComparator())
        )
    }

    @Test
    fun otherSortedByRssiDescending() {
        val networks = listOf(
            wifiNetwork(ssid = "weak", rssi = -80, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3)),
            wifiNetwork(ssid = "strong", rssi = -40, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WEP)),
            wifiNetwork(ssid = "mid", rssi = -60, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3))
        )

        assertEquals(
            listOf(
                wifiNetwork(ssid = "strong", rssi = -40, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WEP)),
                wifiNetwork(ssid = "mid", rssi = -60, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3)),
                wifiNetwork(ssid = "weak", rssi = -80, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3))
            ),
            networks.sortedWith(WiFiNetworkSortComparator())
        )
    }

    @Test
    fun mixedListFullOrder() {
        val networks = listOf(
            wifiNetwork(
                ssid = "other-weak",
                rssi = -80,
                security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3)
            ),
            wifiNetwork(ssid = "supported-weak", rssi = -70, security = WiFiSecurity.Supported.Password.WPA2),
            wifiNetwork(ssid = "other-strong", rssi = -50, security = WiFiSecurity.Other(BsbWifiSecurityMethod.WEP)),
            wifiNetwork(ssid = "supported-strong", rssi = -40, security = WiFiSecurity.Supported.Password.WPA3)
        )

        assertEquals(
            listOf(
                wifiNetwork(ssid = "supported-strong", rssi = -40, security = WiFiSecurity.Supported.Password.WPA3),
                wifiNetwork(ssid = "supported-weak", rssi = -70, security = WiFiSecurity.Supported.Password.WPA2),
                wifiNetwork(
                    ssid = "other-strong",
                    rssi = -50,
                    security = WiFiSecurity.Other(BsbWifiSecurityMethod.WEP)
                ),
                wifiNetwork(
                    ssid = "other-weak",
                    rssi = -80,
                    security = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3)
                )
            ),
            networks.sortedWith(WiFiNetworkSortComparator())
        )
    }

    @Test
    fun sameRssiSecurityAndSsidCompareEqual() {
        val comparator = WiFiNetworkSortComparator()
        val first = wifiNetwork(ssid = "same", rssi = -60, security = WiFiSecurity.Supported.None)
        val second = wifiNetwork(ssid = "same", rssi = -60, security = WiFiSecurity.Supported.None)

        assertEquals(0, comparator.compare(first, second))
        assertEquals(0, comparator.compare(second, first))
    }

    private fun wifiNetwork(
        ssid: String,
        rssi: Int,
        security: WiFiSecurity = WiFiSecurity.Other(
        BsbWifiSecurityMethod.WPA2_WPA3
    )
    ): WiFiNetwork {
        return WiFiNetwork(ssid = ssid, rssi = rssi, wifiSecurity = security)
    }
}
