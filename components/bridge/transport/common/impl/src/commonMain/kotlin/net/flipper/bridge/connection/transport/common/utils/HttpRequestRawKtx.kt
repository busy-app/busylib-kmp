package net.flipper.bridge.connection.transport.common.utils

import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun HttpRequestData.toRawHttpRequestString(
    includeBody: Boolean = true,
    httpVersion: String = "HTTP/1.1"
): String {
    val (bodyBytes, normalizedContent) = captureBodyBytes()

    // Merge user headers + content headers
    val hb = HeadersBuilder().apply {
        appendAll(headers)
        appendAll(normalizedContent.headers)
        normalizedContent.contentType?.let { set(HttpHeaders.ContentType, it.toString()) }
    }

    // Ensure Host header exists
    if (hb[HttpHeaders.Host] == null) {
        val defaultPort = url.protocol.defaultPort
        val hostPort = buildString {
            append(url.host)
            val p = url.port
            if (p != 0 && p != defaultPort) append(":$p")
        }
        hb[HttpHeaders.Host] = hostPort
    }

    // Content-Length if including body
    val bytes = if (includeBody) bodyBytes else ByteArray(0)
    if (includeBody && hb[HttpHeaders.ContentLength] == null) {
        hb[HttpHeaders.ContentLength] = bytes.size.toString()
    }

    val requestTarget = url.encodedPath.ifEmpty { "/" } +
        (if (url.encodedQuery.isEmpty()) "" else "?${url.encodedQuery}")

    val sb = StringBuilder()
    sb.append("${method.value} $requestTarget $httpVersion\r\n")
    val all = hb.build()
    for (name in all.names()) {
        all.getAll(name)?.forEach { value -> sb.append("$name: $value\r\n") }
    }
    sb.append("\r\n")
    if (includeBody && bytes.isNotEmpty()) {
        sb.append(bytes.decodeToString())
    }
    return sb.toString()
}

/** Read request body to bytes *without* sending anything; returns bytes + a normalized OutgoingContent. */
private suspend fun HttpRequestData.captureBodyBytes(): Pair<ByteArray, OutgoingContent> {
    val content: OutgoingContent = body

    val bytes = content.toByteArray()

    // Rebuild a stable content object that matches those bytes (for headers/CT)
    val normalized = object : OutgoingContent.ByteArrayContent() {
        override val contentType: ContentType? = content.contentType
        override val contentLength: Long? = bytes.size.toLong()
        override val headers: Headers = Headers.build {
            content.headers.names().forEach { name ->
                if (!name.equals(HttpHeaders.ContentLength, ignoreCase = true)) {
                    content.headers.getAll(name)?.forEach { append(name, it) }
                }
            }
        }

        override fun bytes(): ByteArray = bytes
    }

    return bytes to normalized
}

private suspend fun OutgoingContent.toByteArray(): ByteArray = when (this) {
    is OutgoingContent.NoContent -> ByteArray(0)
    is OutgoingContent.ByteArrayContent -> bytes()
    is OutgoingContent.ReadChannelContent -> readFrom().readRemaining().readBytes()
    is OutgoingContent.WriteChannelContent -> {
        val ch = ByteChannel(autoFlush = true)
        coroutineScope {
            val w = launch {
                writeTo(ch)
                ch.close()
            }
            val out = ch.readRemaining().readBytes()
            w.join()
            out
        }
    }

    is OutgoingContent.ProtocolUpgrade -> ByteArray(0)
    else -> ByteArray(0)
}
