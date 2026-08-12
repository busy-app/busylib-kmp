package net.flipper.bridge.connection.feature.wifi.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [WiFiNetwork] declares `wifiSecurity` as the sealed [WiFiSecurity] root, which is the position
 * kotlinx would otherwise encode polymorphically — impossible for
 * [WiFiSecurity.Supported.Password], whose descriptor is primitive.
 */
class WiFiNetworkSerializationTest {
    private val json = Json
    private val networkSerializer: KSerializer<WiFiNetwork> = serializer()

    private fun roundTrip(network: WiFiNetwork): WiFiNetwork {
        return json.decodeFromString(
            networkSerializer,
            json.encodeToString(networkSerializer, network)
        )
    }

    @Test
    fun GIVEN_network_with_every_security_WHEN_round_tripped_THEN_it_is_unchanged() {
        val securities = WiFiSecurity.Supported.Password.entries +
            WiFiSecurity.Supported.None +
            BsbWifiSecurityMethod.entries.map { method -> WiFiSecurity.Other(method) }

        securities.forEach { security ->
            val network = WiFiNetwork(
                ssid = "busy-$security",
                rssi = -42,
                wifiSecurity = security
            )

            assertEquals(network, roundTrip(network), security.toString())
        }
    }
}
