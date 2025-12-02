package net.flipper.bridge.connection.transport.mock.impl.model

object ApiDeviceNameResponse {
    fun getJsonPlainTextResponse(): String {
        return """
            {
              "name": "BUSY Bar from API"
            }
        """.trimIndent()
    }

    const val PATH = "/api/name"
}
