package net.flipper.bridge.connection.transport.mock.impl.model

object ApiAccountResponse {
    fun getJsonPlainTextResponse(): String {
        return """
            {
                "state": "not_linked"
            }
        """.trimIndent()
    }

    const val PATH = "/api/account/info"
}
