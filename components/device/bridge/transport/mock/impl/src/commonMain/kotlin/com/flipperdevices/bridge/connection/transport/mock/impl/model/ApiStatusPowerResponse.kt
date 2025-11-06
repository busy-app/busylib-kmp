package com.flipperdevices.bridge.connection.transport.mock.impl.model

object ApiStatusPowerResponse {
    fun getJsonPlainTextResponse(): String {
        return """
            {
              "state": "discharging",
              "battery_charge": 99,
              "battery_voltage": 4187,
              "battery_current": 0,
              "usb_voltage": 5139
            }
        """.trimIndent()
    }

    const val PATH = "/api/status/power"
}
