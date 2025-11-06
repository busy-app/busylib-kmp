package com.flipperdevices.bridge.connection.transport.mock.impl.model

object ApiWifiStatusResponse {
    fun getJsonPlainTextResponse(): String {
        val responses = listOf(
            getConnectedResponse(),
            getEnabledResponse(),
            getDisabledResponse()
        )
        return responses.random()
    }

    private fun getConnectedResponse(): String {
        return """
            {
              "state": "connected",
              "ssid": "HomeNetwork_5G",
              "security": "WPA2/WPA3",
              "ip_config": {
                "ip_method": "dhcp",
                "ip_type": "ipv4",
                "address": "192.168.1.147"
              }
            }
        """.trimIndent()
    }

    private fun getEnabledResponse(): String {
        return """
            {
              "state": "enabled",
              "ssid": null,
              "security": null,
              "ip_config": null
            }
        """.trimIndent()
    }

    private fun getDisabledResponse(): String {
        return """
            {
              "state": "disabled",
              "ssid": "HomeNetwork_5G",
              "security": "WPA2/WPA3",
              "ip_config": {
                "ip_method": "dhcp",
                "ip_type": "ipv4",
                "address": "192.168.1.147"
              }
            }
        """.trimIndent()
    }

    const val PATH = "/api/wifi/status"
}
