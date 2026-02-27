package net.flipper.core.ktor.util

import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.ContentType

/**
 * Removes OctetStream body to minimize logging message
 */
fun LoggingConfig.tryMinimizeOctetStreamBody(message: String): String {
    return when {
        message.contains("Content-Type: ${ContentType.Application.OctetStream}") -> {
            val iStart = message.indexOf("BODY START")
            val iEnd = message.indexOf("BODY END")
            if (iStart == -1 || iEnd == -1) {
                message
            } else {
                message
                    .replaceRange(iStart, iEnd, "/** OCTET_STREAM BODY **/")
                    .plus("\n")
            }
        }

        else -> message
    }
}
