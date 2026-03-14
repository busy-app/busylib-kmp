package net.flipper.core.ktor.util

import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.ContentType

private fun replaceBody(message: String, replacement: () -> String): String {
    val iStart = message.indexOf("BODY START")
    val iEnd = message.indexOf("BODY END")
    return if (iStart == -1 || iEnd == -1) {
        message
    } else {
        message
            .replaceRange(iStart, iEnd, replacement.invoke())
            .plus("\n")
    }
}

/**
 * Removes OctetStream body to minimize logging message
 */
private fun LoggingConfig.tryMinimizeOctetStreamBody(message: String): String {
    return when {
        message.contains("Content-Type: ${ContentType.Application.OctetStream}") -> {
            replaceBody(message) { "/** OCTET_STREAM BODY **/" }
        }

        else -> message
    }
}

/**
 * Removes Image bytes body to minimize logging message
 */
private fun LoggingConfig.tryMinimizeScreenStreamingBody(message: String): String {
    if (!message.contains("content-type: image/bmp", ignoreCase = true)) return message
    return replaceBody(message) { "/** IMAGE BODY **/" }
}

fun LoggingConfig.minimizeBodyLogMessage(message: String): String {
    return message
        .let { nextMessage -> tryMinimizeOctetStreamBody(nextMessage) }
        .let { nextMessage -> tryMinimizeScreenStreamingBody(nextMessage) }
}
