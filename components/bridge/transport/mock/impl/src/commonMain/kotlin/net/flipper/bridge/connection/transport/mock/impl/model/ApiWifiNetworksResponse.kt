package net.flipper.bridge.connection.transport.mock.impl.model

object ApiWifiNetworksResponse {
    fun getJsonPlainTextResponse(): String {
        return """
            {
              "count": 3,
              "networks": [
                {
                  "ssid": "MockNetwork_5G",
                  "security": "WPA3",
                  "rssi": -45
                },
                {
                  "ssid": "MockNetwork_2.4G",
                  "security": "WPA2",
                  "rssi": -52
                },
                {
                  "ssid": "OpenNetwork",
                  "security": "Open",
                  "rssi": -68
                }
              ]
            }
        """.trimIndent()
    }

    const val PATH = "/api/wifi/networks"
}
