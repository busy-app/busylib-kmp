package net.flipper.bridge.connection.transport.mock.impl.model

object ApiAccountResponse {
    fun getJsonPlainTextResponse(): String {
        return """
            {
                "linked": "false"
            }
        """.trimIndent()
    }

    const val PATH = "/api/account/info"
}
