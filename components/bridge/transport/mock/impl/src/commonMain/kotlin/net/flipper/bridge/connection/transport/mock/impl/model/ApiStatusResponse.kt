package net.flipper.bridge.connection.transport.mock.impl.model

object ApiStatusResponse {
    fun getJsonPlainTextResponse(): String {
        return """
            {
              "system": {
                "serial_number": "2036384854315004002c001e",
                "api_semver": "6.3.0",
                "version": "mock",
                "branch": "dev",
                "build_date": "2025-08-27",
                "commit_hash": "053912da",
                "uptime": "00d 04h 48m 56s",
                "boot_time": 1767225620000
              },
              "power": {
                "state": "discharging",
                "battery_charge": 99,
                "battery_voltage": 4187,
                "battery_current": 0,
                "usb_voltage": 5140
              },
              "ble": {
                "state": "not implemented"
              }
            }
        """.trimIndent()
    }

    const val PATH = "/api/status"
}
