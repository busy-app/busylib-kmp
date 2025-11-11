package net.flipper.bridge.connection.transport.mock.impl.model

object ApiBleStatusResponse {
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
              "connected_device_name": "iPhone Veronika"
            }
        """.trimIndent()
    }

    private fun getEnabledResponse(): String {
        return """
            {
              "state": "enabled"
            }
        """.trimIndent()
    }

    private fun getDisabledResponse(): String {
        return """
            {
              "state": "disabled",
            }
        """.trimIndent()
    }

    const val PATH = "/api/ble/status"
}
