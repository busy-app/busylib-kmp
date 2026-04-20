package net.flipper.bridge.connection.feature.wifi.impl

import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import kotlin.test.Test
import kotlin.test.assertEquals

class WiFiNetworkSortComparatorTest {
    @Test
    fun higherRssiGoesFirst() {
        val networks = listOf(
            wifiNetwork(ssid = "low", rssi = 8),
            wifiNetwork(ssid = "high", rssi = 15),
            wifiNetwork(ssid = "mid", rssi = 10)
        )

        assertEquals(
            listOf(
                wifiNetwork(ssid = "high", rssi = 15),
                wifiNetwork(ssid = "mid", rssi = 10),
                wifiNetwork(ssid = "low", rssi = 8)
            ),
            networks.sortedWith(WiFiNetworkSortComparator())
        )
    }

    @Test
    fun sameRssiKeepsOriginalOrder() {
        val first = wifiNetwork(ssid = "first", rssi = 10)
        val second = wifiNetwork(ssid = "second", rssi = 10)
        val third = wifiNetwork(ssid = "third", rssi = 8)

        assertEquals(
            listOf(first, second, third),
            listOf(first, second, third).sortedWith(WiFiNetworkSortComparator())
        )
    }

    @Test
    fun nullRssiGoesLast() {
        assertEquals(-1, compareNullableRssiDescending(10, null))
        assertEquals(1, compareNullableRssiDescending(null, 10))
        assertEquals(0, compareNullableRssiDescending(null, null))
    }

    private fun wifiNetwork(ssid: String, rssi: Int): WiFiNetwork {
        return WiFiNetwork(
            ssid = ssid,
            rssi = rssi,
            wifiSecurity = WiFiSecurity.Other(BsbWifiSecurityMethod.WPA2_WPA3)
        )
    }
}
