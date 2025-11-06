package com.flipperdevices.bridge.connection.transport.mock.impl.model

object ApiWifiConnectResponse {
    fun getJsonPlainTextResponse(): String {
        return listOf(
            getJsonPlainTextOkResponse(),
            getJsonPlainTextBadResponse(),
        ).random()
    }

    private fun getJsonPlainTextOkResponse(): String {
        return """
            {
              "result": "OK"
            }
        """.trimIndent()
    }

    private fun getJsonPlainTextBadResponse(): String {
        return """
            {
              "error": "Invalid parameter",
              "code": 400
            }
        """.trimIndent()
    }

    const val PATH = "/api/wifi/connect"
}
